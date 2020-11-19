package com.example.frontend.ui.calendar;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import com.example.frontend.CalendarAdapter;
import com.example.frontend.Event;
import com.example.frontend.EventDecorator;
import com.example.frontend.GetAuthActivity;
import com.example.frontend.MainActivity;
import com.example.frontend.R;
import com.example.frontend.ServerCommAsync;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import org.threeten.bp.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class CalendarFragment extends Fragment implements OnDateSelectedListener, View.OnClickListener {
    private static final String TAG ="CalendarFragment";
    private static final String EMPTY_STRING = "";

    private String userToken;
    private String code;

    private MaterialCalendarView calendar;
    private ListView events;
    private Button button;
    private CalendarAdapter adapter;
    private List<Event> eventList = new ArrayList<>();
    private HashMap<CalendarDay, List<Event>> eventMap = new HashMap<>();

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_calendar, container, false);
        userToken = MainActivity.getUser().getUserToken();

        calendar = root.findViewById(R.id.calendar);
        calendar.setDateTextAppearance(View.ACCESSIBILITY_LIVE_REGION_ASSERTIVE);
        calendar.setSelectedDate(LocalDate.now());
        calendar.setOnDateChangedListener(this);

        events = root.findViewById(R.id.lv_events);
        adapter = new CalendarAdapter(getActivity(), eventList);
        events.setAdapter(adapter);
        events.setVisibility(View.INVISIBLE);

        button = root.findViewById(R.id.btn_calendar);
        button.setOnClickListener(this);

        return root;
    }

    @Override
    public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date, boolean selected) {

        calendar.setHeaderTextAppearance(R.style.AppTheme);

        eventList =  eventMap.get(date);
        if(eventList != null && eventList.size() > 0) {
            adapter.addItems(eventList);
        }
        else {
            adapter.clear();
        }
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent(CalendarFragment.this.getContext(), GetAuthActivity.class);
        startActivityForResult(intent, 1);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            // here you can retrieve your bundle data.
            code = data.getStringExtra("code");
            JSONObject JSONcode = new JSONObject();
            try {
                JSONcode.put("code", code);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            getEventsFromServer(JSONcode, userToken);

            while (eventMap.size() == 0) {
                Log.d(TAG, "waiting for events");
            }
            eventList = eventMap.get(CalendarDay.from(LocalDate.now().minusMonths(2)));

            if (eventList != null && eventList.size() > 0) {
                adapter.addItems(eventList);
            }
            else {
                adapter.clear();
            }

            //add small dots on event days
            EventDecorator eventDecorator = new EventDecorator(Color.RED, eventMap.keySet());
            calendar.addDecorator(eventDecorator);

            button.setVisibility(View.GONE);
            events.setVisibility(View.VISIBLE);
        }
    }

    private void getEventsFromServer(final JSONObject codeJSON, String userToken) {
        ServerCommAsync serverComm = new ServerCommAsync();

        serverComm.postWithAuthentication("http://closet-cpen321.westus.cloudapp.azure.com/api/calendar/Oct-2020", codeJSON.toString(), userToken, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseStr = Objects.requireNonNull(response.body()).string();
                    try {
                        JSONArray responseJSON = new JSONArray(responseStr);
                        extractResponseEventData(responseJSON);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                else {

                }
            }
        });
    }

    private void extractResponseEventData(JSONArray responseJSON) throws JSONException {
        LocalDate startDate = LocalDate.now(), endDate = LocalDate.now();
        String summary = EMPTY_STRING;

        for (int i = 0; i < responseJSON.length(); i++) {
            JSONObject eventJSON = responseJSON.getJSONObject(i);
            if (eventJSON.has("start")) {
                if (eventJSON.getJSONObject("start").has("date")) {
                    startDate = LocalDate.parse(eventJSON.getJSONObject("start").getString("date"));
                }
                else {
                    startDate = LocalDate.parse(eventJSON.getJSONObject("start").getString("dateTime").substring(0, 10));
                }
            }

            if (eventJSON.has("end")) {
                if (eventJSON.getJSONObject("end").has("date")) {
                    endDate = LocalDate.parse(eventJSON.getJSONObject("end").getString("date"));
                }
                else {
                    endDate = LocalDate.parse(eventJSON.getJSONObject("end").getString("dateTime").substring(0, 10));
                }
            }

            if (eventJSON.has("summary")) {
                summary = eventJSON.getString("summary");
            }

            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                Event event = new Event(date, summary);
                CalendarDay day = CalendarDay.from(date);
                if (!eventMap.containsKey(day)) {
                    List<Event> events = new ArrayList<>();
                    events.add(event);
                    eventMap.put(day, events);
                }
                else {
                    List<Event> events = eventMap.get(day);
                    events.add(event);
                    eventMap.put(day, events);
                }
            }
        }
    }
}