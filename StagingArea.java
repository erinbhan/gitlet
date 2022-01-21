package gitlet;
import java.io.Serializable;
import java.util.LinkedHashMap;

/**
 * StagingArea class for Gitlet, the tiny stupid version-control system.
 *
 * @author Erin Bhan
 */
public class StagingArea implements Serializable {
    /** Add File.*/
    private LinkedHashMap<String, String> addFile;
    /** Remove File.*/
    private LinkedHashMap<String, String> removeFile;

    /** Staging area.*/
    public StagingArea() {
        addFile = new LinkedHashMap<>();
        removeFile = new LinkedHashMap<>();
    }

    /** StagingARea class.
     * @param fileName This is file name.
     * @param shaCode This is shaCode.
     * */
    public void add(String fileName, String shaCode) {
        addFile.put(fileName, shaCode);
    }

    /** StagingARea class.
     * @param fileName This is file name.
     * */
    public void add(String fileName) {
        addFile.put(fileName, "");
    }

    /** Staging Area class.
     * @param fileName This is file name.
     * @param shaCode This is shaCode.
     * */
    public void remove(String fileName, String shaCode) {
        removeFile.put(fileName, shaCode);

    }

    /** StagingARea class.
     * @param fileName This is file name.
     * */
    public void remove(String fileName) {
        removeFile.put(fileName, "");
    }

    /** StagingARea class.
     * @param fileName This is file name.
     * */
    public void removeAdd(String fileName) {
        addFile.remove(fileName);
    }

    /** StagingARea class.
     * @param fileName This is file name.
     * */
    public void removeRemove(String fileName) {
        removeFile.remove(fileName);
    }

    /** StagingARea class.*/
    public void clear() {
        addFile = new LinkedHashMap<>();
        removeFile = new LinkedHashMap<>();
    }

    /** StagingARea class.
     * @return Add file.*/
    public LinkedHashMap<String, String> getAddFiles() {
        return addFile;
    }
    /** StagingARea class.
     * @return Remove File.
     * */
    public LinkedHashMap<String, String> getRemoveFiles() {
        return removeFile;
    }
}
