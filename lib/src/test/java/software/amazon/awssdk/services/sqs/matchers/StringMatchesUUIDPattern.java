package software.amazon.awssdk.services.sqs.matchers;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class StringMatchesUUIDPattern extends TypeSafeMatcher<String> {
    private static final String UUID_REGEX = "[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}";

    @Override
    protected boolean matchesSafely(String s) {
        return s.matches(UUID_REGEX);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("a string matching the pattern of a UUID");
    }

    @Factory
    public static Matcher<String> matchesThePatternOfAUUID() {
        return new StringMatchesUUIDPattern();
    }

}