package gitlet;
import java.io.Serializable;
import java.util.Date;
import java.util.LinkedHashMap;
/**
 * Commit class for Gitlet, the tiny stupid version-control system.
 *
 * @author Erin Bhan
 */
public class Commit implements Serializable {

    /** Commit class message.*/
    private String message;
    /** Commit class timestamp.*/
    private Date timestamp;
    /** Commit class linkedhashmap.*/
    private LinkedHashMap<String, String> track;
    /** Commit class parent.*/
    private String parent;
    /** Commit class parent2.*/
    private String parent2;
    /** Commit class branch.*/
    private String branch;
    /** Commit class branch2.*/
    private String branch2;

    /** Commit class.
     * @param message1 The message.
     * @param parents The parents for commit.
     * @param parent2s The second parents.
     * */

    public Commit(String message1, String parents, String parent2s) {
        this.message = message1;
        this.parent = parents;
        this.parent2 = parent2s;
        this.timestamp = new Date();
        if (parent == null) {
            this.timestamp = new Date(0);
        }
        track = new LinkedHashMap<>();
    }

    /** String for getMessage.
     * @return Returns a message.
     * */
    public String getMessage() {
        return this.message;
    }

    /** Gets the timestamp.
     * @return Returns the timestamp
     * */
    public Date getTimestamp() {
        return this.timestamp;
    }

    /** Gets the first parent.
     * @return Returns the first parent.
     * */
    public String getParent() {
        return this.parent;
    }

    /** Sets the first parent.
     * @param parents The parents.
     * */
    public void setParent(String parents) {
        this.parent = parents;
    }

    /** Gets the second parent.
     * @return Returns the second parent.
     * */
    public String getParent2() {
        return this.parent2;
    }

    /** Set the second parent.
     * @param parent2s The second parent.
     * */
    public void setParent2(String parent2s) {
        this.parent2 = parent2s;
    }

    /** Edits the hash.
     * @param s The stagingArea.*/
    public void editHash(StagingArea s) {
        LinkedHashMap<String, String> addFiles = s.getAddFiles();
        LinkedHashMap<String, String> rmFiles = s.getRemoveFiles();
        for (String f : addFiles.keySet()) {
            track.put(f, addFiles.get(f));
        }
        for (String rem : rmFiles.keySet()) {
            track.remove(rem);
        }
    }

    /** Combines the commits.
     * @param other The other for commit.
     * */
    public void combine(Commit other) {
        for (String s: other.track.keySet()) {
            track.put(s, other.track.get(s));
        }
    }

    /** LinkedHashMap for commits.
     * @return Returns the track.
     * */
    public LinkedHashMap<String, String> getHashMap() {
        return track;
    }

    /** Gets the branch.
     * @return Returns the branch.
     * */
    public String getBranch() {
        return branch;
    }

    /** Sets the branch.
     * @param branch1 The first branch.
     * */
    public void setBranch(String branch1) {
        this.branch = branch1;
    }

    /** Sets the second branch.
     * @param branch2s The second branch.
     * */
    public void setBranch2(String branch2s) {
        this.branch2 = branch2s;
    }
}
