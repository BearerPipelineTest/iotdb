package cn.edu.tsinghua.iotdb.query.reader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cn.edu.tsinghua.tsfile.common.utils.ITsRandomAccessFileReader;
import cn.edu.tsinghua.tsfile.file.metadata.RowGroupMetaData;
import cn.edu.tsinghua.tsfile.timeseries.read.TsRandomAccessLocalFileReader;


/**
 * This class is used to construct FileReader. <br>
 * It is an adapter between {@code RecordReader} and {@code FileReader}
 *
 * @author Jinrui Zhang, CGF
 */

public class ReaderManager {

    private List<FileReader> fileReaderList;

    private List<ITsRandomAccessFileReader> rafList; // file has been serialized, sealed

    private HashMap<String, List<RowGroupReader>> rowGroupReaderMap;
    private List<RowGroupReader> rowGroupReaderList;


    /**
     * {NEWFUNC}
     *
     * @param rafList fileInputStreamList
     * @throws IOException
     */
    ReaderManager(List<ITsRandomAccessFileReader> rafList) throws IOException {
        this.rafList = rafList;
        rowGroupReaderList = new ArrayList<>();
        rowGroupReaderMap = new HashMap<>();
        fileReaderList = new ArrayList<>();

        for (ITsRandomAccessFileReader taf : rafList) {
            FileReader fr = new FileReader(taf);
            fileReaderList.add(fr);
            addRowGroupReadersToMap(fr);
            addRowGroupReadersToList(fr);
        }
    }

    /**
     * {NEWFUNC}
     *
     * @param rafList               file node list
     * @param unsealedFileReader fileReader for unsealedFile
     * @param rowGroupMetadataList  RowGroupMetadata List for unsealedFile
     * @throws IOException
     */
    ReaderManager(List<ITsRandomAccessFileReader> rafList,
                         ITsRandomAccessFileReader unsealedFileReader, List<RowGroupMetaData> rowGroupMetadataList) throws IOException {
        this(rafList);
        this.rafList.add(unsealedFileReader);

        FileReader fr = new FileReader(unsealedFileReader, rowGroupMetadataList);
        addRowGroupReadersToMap(fr);
        addRowGroupReadersToList(fr);
    }

    private void addRowGroupReadersToMap(FileReader fileReader) {
        HashMap<String, ArrayList<RowGroupReader>> rgrMap = fileReader.getRowGroupReadersMap();
        for (String deltaObjectUID : rgrMap.keySet()) {
            if (rowGroupReaderMap.containsKey(deltaObjectUID)) {
                rowGroupReaderMap.get(deltaObjectUID).addAll(rgrMap.get(deltaObjectUID));
            } else {
                rowGroupReaderMap.put(deltaObjectUID, rgrMap.get(deltaObjectUID));
            }
        }
    }

    private void addRowGroupReadersToList(FileReader fileReader) {
        this.rowGroupReaderList.addAll(fileReader.getRowGroupReaderList());
    }

    List<RowGroupReader> getAllRowGroupReaders() {
        return rowGroupReaderList;
    }

    List<RowGroupReader> getRowGroupReaderListByDeltaObject(String deltaObjectUID) {
        List<RowGroupReader> ret = rowGroupReaderMap.get(deltaObjectUID);
        if (ret == null) {
            return new ArrayList<>();
        }
        return ret;
    }

    HashMap<String, List<RowGroupReader>> getRowGroupReaderMap() {
        return rowGroupReaderMap;
    }

    /**
     * @throws IOException
     */
    public void close() throws IOException {
        for (ITsRandomAccessFileReader raf : rafList) {
            if (raf instanceof TsRandomAccessLocalFileReader) {
                ((TsRandomAccessLocalFileReader) raf).closeFromManager();
            } else {
                raf.close();
            }
        }
    }
}
