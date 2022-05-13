public class BlockMeta {
    private int createTerm;
    private String creator;

    public BlockMeta(int createTerm, String creator) {
        this.createTerm = createTerm;
        this.creator = creator;
    }

    public int getCreateTerm() {
        return createTerm;
    }

    public String getCreator() {
        return creator;
    }
}
