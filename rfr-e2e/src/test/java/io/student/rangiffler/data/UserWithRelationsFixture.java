package io.student.rangiffler.data;

/**
 * Adds partners linked in one {@link Relation}, each a user of its own, to the user of another fixture.
 */
public final class UserWithRelationsFixture implements UserFixture {

    private final UserFixture origin;
    private final Relation relation;
    private final Partners partners;

    private UserWithRelationsFixture(UserFixture origin, Relation relation, Partners partners) {
        this.origin = origin;
        this.relation = relation;
        this.partners = partners;
    }

    /**
     * Creates the partners and their friendships, and closes everything created so far, the origin included, if that fails.
     */
    public static UserWithRelationsFixture create(UserFixture origin, Relation relation, int count) {
        return new UserWithRelationsFixture(
                origin,
                relation,
                Partners.create(origin, count, partner -> relation.friendship(origin.user(), partner))
        );
    }

    @Override
    public TestUser user() {
        return relation.attach(origin.user(), partners.users());
    }

    /**
     * Removes the partners and then the origin user.
     */
    @Override
    public void close() {
        partners.close();
    }
}
