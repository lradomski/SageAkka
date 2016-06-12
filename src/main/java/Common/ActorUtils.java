package common;

import java.util.regex.Pattern;

public class ActorUtils {

    static private final Pattern actorNameIllegalChars = Pattern.compile("[^a-zA-Z0-9-_\\.\\*\\$\\+:@&=,!~']");

    public static String makeActorName(String name)
    {
        return actorNameIllegalChars.matcher(name).replaceAll("");
    }
}
