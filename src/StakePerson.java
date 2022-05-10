public class StakePerson {
    private String stake_person;
    private int stake_amount;

    public StakePerson(String person, int amount) {
        this.stake_person = person;
        this.stake_amount = amount;
    }

    public String getStake_person() {
        return stake_person;
    }

    public void setStake_person(String stake_person) {
        this.stake_person = stake_person;
    }

    public int getStake_amount() {
        return stake_amount;
    }

    public void setStake_amount(int stake_amount) {
        this.stake_amount = stake_amount;
    }
}
