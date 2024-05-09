package simpledb;

import java.io.*;
import java.util.*;

/**
 * HeapFile is an implementation of a DbFile that stores a collection of tuples
 * in no particular order. Tuples are stored on pages, each of which is a fixed
 * size, and the file is simply a collection of those pages. HeapFile works
 * closely with HeapPage. The format of HeapPages is described in the HeapPage
 * constructor.
 * 
 * @see simpledb.HeapPage#HeapPage
 * @author Sam Madden
 */
public class HeapFile implements DbFile {

	/**
     * write inner class:DbFileIterator is the iterator interface that all SimpleDB Dbfile should
     */
	private static final class HeapFileIterator implements DbFileIterator {
        private final HeapFile heapFile;
        private final TransactionId tid;

        /**
         * iterator
         */
        private Iterator<Tuple> tupleIterator;
        private int index;

        public HeapFileIterator(HeapFile file,TransactionId tid){
            this.heapFile = file;
            this.tid = tid;
        }
        @Override
        public void open() throws DbException, TransactionAbortedException {
            index = 0;
            tupleIterator = getTupleIterator(index);
        }

        private Iterator<Tuple> getTupleIterator(int pageNumber) throws TransactionAbortedException, DbException{
            if(pageNumber >= 0 && pageNumber < heapFile.numPages()){
                HeapPageId pid = new HeapPageId(heapFile.getId(),pageNumber);
                HeapPage page = (HeapPage)Database.getBufferPool().getPage(tid, pid, Permissions.READ_ONLY);
                return page.iterator();
            }else{
                throw new DbException(String.format("heapFile %d  does not exist in page[%d]!", pageNumber,heapFile.getId()));
            }
        }

        @Override
        public boolean hasNext() throws DbException, TransactionAbortedException {
            // TODO Auto-generated method stub

            if(tupleIterator == null){
                return false;
            }

            while(!tupleIterator.hasNext()){
                index++;
                if(index < heapFile.numPages()){
                    tupleIterator = getTupleIterator(index);
                }else{
                    return false;
                }
            }
            return true;

        }

        @Override
        public Tuple next() throws DbException, TransactionAbortedException, NoSuchElementException {
            if(tupleIterator == null || !tupleIterator.hasNext()){
                throw new NoSuchElementException();
            }
            return tupleIterator.next();
        }

        @Override
        public void rewind() throws DbException, TransactionAbortedException {
            close();
            open();
        }

        @Override
        public void close() {
            tupleIterator = null;
        }

    }
	
    /**
     * Constructs a heap file backed by the specified file.
     * 
     * @param f
     *            the file that stores the on-disk backing store for this heap
     *            file.
     */
	private final File f;
	private final TupleDesc td;
    public HeapFile(File f, TupleDesc td) {
        // some code goes here
    	this.f = f;
    	this.td = td;
    }

    /**
     * Returns the File backing this HeapFile on disk.
     * 
     * @return the File backing this HeapFile on disk.
     */
    public File getFile() {
        // some code goes here
        return this.f;
    }

    /**
     * Returns an ID uniquely identifying this HeapFile. Implementation note:
     * you will need to generate this tableid somewhere ensure that each
     * HeapFile has a "unique id," and that you always return the same value for
     * a particular HeapFile. We suggest hashing the absolute file name of the
     * file underlying the heapfile, i.e. f.getAbsoluteFile().hashCode().
     * 
     * @return an ID uniquely identifying this HeapFile.
     */
    public int getId() {
        // some code goes here
        //throw new UnsupportedOperationException("implement this");
    	return f.getAbsoluteFile().hashCode();
    }

    /**
     * Returns the TupleDesc of the table stored in this DbFile.
     * 
     * @return TupleDesc of this DbFile.
     */
    public TupleDesc getTupleDesc() {
        // some code goes here
        //throw new UnsupportedOperationException("implement this");
    	return this.td;
    }

    // see DbFile.java for javadocs
    public Page readPage(PageId pid) {
        // some code goes here
    	int tableId = pid.getTableId();
        int pgNo = pid.pageNumber();
        int offset = pgNo * BufferPool.getPageSize();
        RandomAccessFile randomAccessFile = null;

        try{
            randomAccessFile = new RandomAccessFile(f,"r");
            // have the amount of pgNo, it should bigger than pgNo
            if((long) (pgNo + 1) *BufferPool.getPageSize() > randomAccessFile.length()){
                randomAccessFile.close();
                throw new IllegalArgumentException(String.format("table %d page %d is invalid", tableId, pgNo));
            }
            byte[] bytes = new byte[BufferPool.getPageSize()];
            // swift the offset to the start of the file, and calculate whether it change
            randomAccessFile.seek(offset);
            int read = randomAccessFile.read(bytes,0,BufferPool.getPageSize());
            // Do not load the entire table into memory on the open() call
            // -- this will cause an out of memory error for very large tables.
            if(read != BufferPool.getPageSize()){
                throw new IllegalArgumentException(String.format("table %d page %d read %d bytes not equal to BufferPool.getPageSize() ", tableId, pgNo, read));
            }
            HeapPageId id = new HeapPageId(pid.getTableId(),pid.pageNumber());
            return new HeapPage(id,bytes);
        }catch (IOException e){
            e.printStackTrace();
        }finally {
            try{
                if(randomAccessFile != null){
                    randomAccessFile.close();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        throw new IllegalArgumentException(String.format("table %d page %d is invalid", tableId, pgNo));
    }

    // see DbFile.java for javadocs
    public void writePage(Page page) throws IOException {
        // some code goes here
        // not necessary for lab1
    	PageId pageId = page.getId();
        int pageNo = pageId.pageNumber();
        int offset = pageNo * BufferPool.getPageSize();
        byte[] pageData = page.getPageData();

        RandomAccessFile file = new RandomAccessFile(this.f, "rw");
        file.seek(offset);
        file.write(pageData);
        file.close();

        page.markDirty(false, null);
    }

    /**
     * Returns the number of pages in this HeapFile.
     */
    public int numPages() {
        // some code goes here
    	//System.out.println(String.format("file length :「 %d 」,BufferPool PageSize :「 %d 」 ",getFile().length(),BufferPool.getPageSize()));
        return (int) Math.floor(getFile().length() * 1.0 / BufferPool.getPageSize());
    }

    // see DbFile.java for javadocs
    public ArrayList<Page> insertTuple(TransactionId tid, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        // some code goes here
        //return null;
        // not necessary for lab1
    	ArrayList<Page> pageList= new ArrayList<Page>();
        for(int i=0;i<numPages();++i){
            // took care of getting new page
            HeapPage p = (HeapPage) Database.getBufferPool().getPage(tid,
                    new HeapPageId(this.getId(),i),Permissions.READ_WRITE);
            if(p.getNumEmptySlots() == 0)
                continue;
            p.insertTuple(t);
            pageList.add(p);
            return pageList;
        }
        // if no slot, start a new page
        BufferedOutputStream bw = new BufferedOutputStream(new FileOutputStream(f,true));
        byte[] emptyData = HeapPage.createEmptyPageData();
        bw.write(emptyData);
        bw.close();
        // load into BufferPool
        HeapPage p = (HeapPage) Database.getBufferPool().getPage(tid,
                new HeapPageId(getId(),numPages()-1),Permissions.READ_WRITE);
        p.insertTuple(t);
        pageList.add(p);
        return pageList;
    }

    // see DbFile.java for javadocs
    public ArrayList<Page> deleteTuple(TransactionId tid, Tuple t) throws DbException,
            TransactionAbortedException {
        // some code goes here
        //return null;
        // not necessary for lab1
    	//HeapPage page = (HeapPage) Database.getBufferPool().getPage(tid,
        //        t.getRecordId().getPageId(),Permissions.READ_WRITE);
        //page.deleteTuple(t);
        //return (ArrayList)Collections.singletonList(page);
        //return Arrays.asList(page);
        
        ArrayList<Page> list = new ArrayList<>();
        PageId pageId = t.getRecordId().getPageId();
        HeapPage page = (HeapPage)Database.getBufferPool().getPage(tid, pageId, Permissions.READ_WRITE);
        page.deleteTuple(t);
        list.add(page);
        return list;
    }

    // see DbFile.java for javadocs
    public DbFileIterator iterator(TransactionId tid) {
        // some code goes here
        return new HeapFileIterator(this, tid);
    }

}

