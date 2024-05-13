public class Result {

    private String id;
    private boolean status;

    public Result(String id, boolean status) {

        this.id = id;
        this.status = status;
    }

    public String toString() {

        return id;
    }

    public boolean getStatus() {

        return status;
    }
}
