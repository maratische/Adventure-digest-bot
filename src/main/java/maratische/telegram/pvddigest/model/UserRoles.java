package maratische.telegram.pvddigest.model;

public enum UserRoles {

    BANNED(0),
    BEGINNER(1),//начинающий
    TRAVELER(2),//путешественник
    ADVANCED(3),//продвинутый
    MODERATOR(4),//модератор
    ADMIN(5);//админ

    private final int value;

    UserRoles(final int newValue) {
        value = newValue;
    }
}
