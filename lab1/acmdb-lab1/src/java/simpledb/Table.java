package simpledb;

//import lombok.Data;
import simpledb.DbFile;

//@Data
public class Table {

    /**
     * the contents of the table to add;  file.getId() is the identfier of
     * this file/tupledesc param for the calls getTupleDesc and getFile
     */
    private DbFile file;

    /**
     * the name of the table -- may be an empty string.  May not be null.  If a name
     * conflict exists, use the last table to be added as the table for a given name.
     */
    private String name;

    /**
     * the name of the primary key field
     */
    private String pkeyField;

    public Table(DbFile file,String name,String pkeyField){
        this.file = file;
        this.name = name;
        this.pkeyField = pkeyField;
    }

    public Table(DbFile file,String name){
        new Table(file,name,"");
    }

    public DbFile getFile(){
    	return this.file;
    }
    
    public String getName() {
    	return this.name;
    }
    
    public String getPkeyField() {
    	return this.pkeyField;
    }
}

