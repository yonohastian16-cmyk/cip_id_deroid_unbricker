package com.jejakteknisi.gradeemmc;
import android.app.*; import android.os.*; import android.content.*; import android.database.sqlite.*; import android.database.Cursor; import android.graphics.Color; import android.view.*; import android.widget.*; import java.io.*; import java.util.*;
public class MainActivity extends Activity {
 DB db; LinearLayout list; EditText search; Spinner filter; TextView count; ArrayList<Item> data=new ArrayList<>();
 public void onCreate(Bundle b){super.onCreate(b);db=new DB(this);build();load();}
 TextView tv(String s,int sp){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setPadding(18,12,18,12);return t;}
 void build(){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(12,12,12,8);
  TextView title=tv("JEJAK TEKNISI\nGRADE eMMC",22);title.setTextColor(Color.rgb(0,120,80));title.setTypeface(null,1);root.addView(title);
  LinearLayout bar=new LinearLayout(this);search=new EditText(this);search.setHint("🔍 Cari kode / grade / kapasitas");search.setSingleLine(true);bar.addView(search,new LinearLayout.LayoutParams(0,58,1));
  Button add=new Button(this);add.setText("+ DATA");bar.addView(add,new LinearLayout.LayoutParams(-2,58));root.addView(bar);
  filter=new Spinner(this);String[] fs={"SEMUA GRADE","A+++","A++","A+","A+B","Pilihan 256","Pilihan 128","Pilihan 64","Pilihan 32","Pilihan 16","Pilihan 8","A+ Samsung/A Husus"};
  filter.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,fs));root.addView(filter);count=tv("",14);root.addView(count);
  ScrollView sv=new ScrollView(this);list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);sv.addView(list);root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
  search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int d){}public void onTextChanged(CharSequence s,int a,int b,int c){load();}public void afterTextChanged(android.text.Editable e){}});
  filter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){load();}});
  add.setOnClickListener(v->dialog(null));
 }
 void load(){data.clear();list.removeAllViews();String q=search.getText().toString().trim().toUpperCase();String g=filter.getSelectedItem()==null?"SEMUA GRADE":filter.getSelectedItem().toString();Cursor c=db.query(q,g);while(c.moveToNext())data.add(new Item(c.getLong(0),c.getString(1),c.getString(2),c.getString(3)));c.close();count.setText(data.size()+" data");
  for(Item x:data){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.VERTICAL);row.setPadding(8,4,8,4);TextView a=tv(x.code,18);a.setTypeface(null,1);TextView b=tv(x.grade+"   •   "+(x.cap.length()>0?x.cap+" GB":"Kapasitas -"),14);row.addView(a);row.addView(b);row.setBackgroundColor(Color.rgb(245,245,245));row.setOnClickListener(v->detail(x));list.addView(row);}
 }
 void detail(Item x){new AlertDialog.Builder(this).setTitle(x.code).setMessage("Grade: "+x.grade+"\nKapasitas: "+(x.cap.length()>0?x.cap+" GB":"-")).setPositiveButton("EDIT",(d,w)->dialog(x)).setNegativeButton("HAPUS",(d,w)->{db.del(x.id);load();}).setNeutralButton("TUTUP",null).show();}
 void dialog(Item edit){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(30,5,30,5);EditText code=new EditText(this);code.setHint("Kode eMMC");box.addView(code);EditText grade=new EditText(this);grade.setHint("Grade");box.addView(grade);EditText cap=new EditText(this);cap.setHint("Kapasitas GB");cap.setInputType(2);box.addView(cap);if(edit!=null){code.setText(edit.code);grade.setText(edit.grade);cap.setText(edit.cap);}
  new AlertDialog.Builder(this).setTitle(edit==null?"Tambah Data":"Edit Data").setView(box).setPositiveButton("SIMPAN",(d,w)->{if(code.getText().toString().trim().isEmpty())return;if(edit==null)db.add(code.getText().toString().trim().toUpperCase(),grade.getText().toString().trim(),cap.getText().toString().trim());else db.upd(edit.id,code.getText().toString().trim().toUpperCase(),grade.getText().toString().trim(),cap.getText().toString().trim());load();}).setNegativeButton("BATAL",null).show();}
 static class Item{long id;String code,grade,cap;Item(long i,String c,String g,String p){id=i;code=c;grade=g;cap=p;}}
}
class DB extends SQLiteOpenHelper{
 Context ctx;DB(Context c){super(c,"grade_emmc.db",null,1);ctx=c;}
 public void onCreate(SQLiteDatabase d){d.execSQL("CREATE TABLE emmc(id INTEGER PRIMARY KEY AUTOINCREMENT,code TEXT NOT NULL,grade TEXT NOT NULL,capacity TEXT)");importCsv(d);}
 void importCsv(SQLiteDatabase d){try{BufferedReader r=new BufferedReader(new InputStreamReader(ctx.getAssets().open("grade_emmc.csv")));String s;r.readLine();while((s=r.readLine())!=null){String[] a=s.split(",",-1);if(a.length>=3)d.execSQL("INSERT INTO emmc(code,grade,capacity) VALUES(?,?,?)",new Object[]{a[0],a[1],a[2]});}r.close();}catch(Exception e){}}
 Cursor query(String q,String g){ArrayList<String>a=new ArrayList<>(Arrays.asList("%"+q+"%","%"+q+"%","%"+q+"%"));String where="(code LIKE ? OR grade LIKE ? OR capacity LIKE ?)";if(!g.equals("SEMUA GRADE")){where+=" AND grade LIKE ?";a.add("%"+g+"%");}return getReadableDatabase().rawQuery("SELECT id,code,grade,capacity FROM emmc WHERE "+where+" ORDER BY grade,code",a.toArray(new String[0]));}
 void add(String c,String g,String p){getWritableDatabase().execSQL("INSERT INTO emmc(code,grade,capacity) VALUES(?,?,?)",new Object[]{c,g,p});}void upd(long id,String c,String g,String p){getWritableDatabase().execSQL("UPDATE emmc SET code=?,grade=?,capacity=? WHERE id=?",new Object[]{c,g,p,id});}void del(long id){getWritableDatabase().delete("emmc","id=?",new String[]{""+id});}public void onUpgrade(SQLiteDatabase d,int a,int b){}
}