package com.example.frontend;


import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.idling.CountingIdlingResource;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.filters.LargeTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.withDecorView;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class RegisterTest {
    private View decorView;


    @Rule
    public ActivityScenarioRule<RegisterActivity> activityRule
            = new ActivityScenarioRule<>(RegisterActivity.class);
    @Before
    public void setUp() {
        activityRule.getScenario().onActivity(new ActivityScenario.ActivityAction<RegisterActivity>() {
            @Override
            public void perform(RegisterActivity activity) {
                decorView = activity.getWindow().getDecorView();
            }
        });
    }
    @Test
    public void registerWithExistingAccountTest() {
        //we register and unregister idling resources  with Espresso to validate asynchronous operations
        // such as send register data to server and wait for response before proceeding to next part of UI test
        CountingIdlingResource componentIdlingResource = RegisterActivity.getRegisterIdlingResourceInTest();
        IdlingRegistry idlingRegistry = IdlingRegistry.getInstance();
        idlingRegistry.register(componentIdlingResource);

        //check if all components on the register UI are there
        onView(withId(R.id.etName)).check(matches(isDisplayed()));
        onView(withId(R.id.etEmail)).check(matches(isDisplayed()));
        onView(withId(R.id.etPassword)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_register)).check(matches(isDisplayed()));
        onView(withId(R.id.linkToLogin)).check(matches(withText("Already have an account? Login here")));

        //Try to register with an already existing account
        ViewInteraction appCompatEditText = onView(
                allOf(withId(R.id.etName),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                2),
                        isDisplayed()));
        appCompatEditText.perform(replaceText("test"), closeSoftKeyboard());

        ViewInteraction appCompatEditText2 = onView(
                allOf(withId(R.id.etEmail),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                3),
                        isDisplayed()));
        appCompatEditText2.perform(replaceText("test@test.com"), closeSoftKeyboard());

        ViewInteraction appCompatEditText3 = onView(
                allOf(withId(R.id.etPassword),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                1),
                        isDisplayed()));
        appCompatEditText3.perform(replaceText("123123"), closeSoftKeyboard());

        ViewInteraction appCompatButton = onView(
                allOf(withId(R.id.btn_register), withText("Register"),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                0),
                        isDisplayed()));
        appCompatButton.perform(click());

        //check if a toast message“User exists already, please login instead” is present
        onView(withText("User exists already, please login instead")).inRoot(withDecorView(not(decorView))).check(matches(withText("User exists already, please login instead")));

        //should stay at the register activity since we fail to register
        //check if all components on the register UI are there
        onView(withId(R.id.etName)).check(matches(isDisplayed()));
        onView(withId(R.id.etEmail)).check(matches(isDisplayed()));
        onView(withId(R.id.etPassword)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_register)).check(matches(isDisplayed()));
        onView(withId(R.id.linkToLogin)).check(matches(withText("Already have an account? Login here")));

        ViewInteraction appCompatTextView = onView(
                allOf(withId(R.id.linkToLogin), withText("Already have an account? Login here"),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                4),
                        isDisplayed()));
        appCompatTextView.perform(click());

        //jump to LoginActivity to try to login
        //check if all components on the Login UI are there
        onView(withId(R.id.etEmail_login)).check(matches(isDisplayed()));
        onView(withId(R.id.etPassword_login)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_login)).check(matches(isDisplayed()));
        onView(withId(R.id.linkToRegister)).check(matches(withText("New here? Create an Account here")));

        //unregister idling resources with Espresso
        idlingRegistry.unregister(componentIdlingResource);

    }

    private static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }
}
