package com.example.gokhan.papurrless;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    private static String tag = "Main Activity";
    private SharedPreferences.Editor editor;

    /**
     * The {@link PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    private ListFragment.AllFragment allFrag;
    private ListFragment.FavFragment favFrag;

    private final int TAKE_PICTURE = 0;
    private final int SELECT_FILE = 1;

    private String imageFilePath;
    private String outputPath = "result.txt";
    private List<ParseObject> queuedForUpload = new ArrayList();
    private boolean premiumEnabled = false;

    ParseUser user = ParseUser.getCurrentUser();
    ParseObject img;

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.premiumEnabled_key), Context.MODE_PRIVATE);
        premiumEnabled = sharedPref.getBoolean(getString(R.string.premiumEnabled), false);
        editor = sharedPref.edit();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);


        final CharSequence opts[] = new CharSequence[]{"Camera", "Storage"};
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select image from");
        builder.setItems(opts, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        captureImageFromCamera();
                        break;
                    case 1:
                        captureImageFromSdCard();
                        break;
                }
            }
        });
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                builder.show();
            }
        });

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem loginMenuItem = menu.findItem(R.id.action_login);
        MenuItem uploadItem = menu.findItem(R.id.action_offline_files);

        if(user != null)
        {
            loginMenuItem.setTitle(R.string.action_log_out);
        }



        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_login) {
            if(user != null)
                logOut();
            else
                logIn();
            return true;
        }else if(id == R.id.action_offline_files){
            allFrag.loadOfflineFiles(this, false);
        }

        return super.onOptionsItemSelected(item);
    }

    public void logIn() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
    }

    /**
     *
     * This method crashes at version 6.0
     * http://stackoverflow.com/questions/32561479/android-studio-getslotfrombufferlocked-unknown-buffer-error/33146256
     * */
    public void logOut() {

        user.logOut();
        Toast.makeText(this, R.string.toast_logged_out, Toast.LENGTH_SHORT).show();
        this.recreate();
    }

    public String getDate(){

        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String strDate = sdf.format(c.getTime());
        return strDate;
    }

    public void saveDataToStorage(ArrayList<String> data){

        try{
            File mediaStorageDir = new File(Environment.getExternalStorageDirectory(), "Papurrless");
            if (!mediaStorageDir.exists()) {
                mediaStorageDir.mkdirs();
            }

            String dateTaken = getDate();

            File file = new File(mediaStorageDir.getPath() + File.separator + "scanned-data" + dateTaken + ".txt");
            file.createNewFile();

            FileWriter fw = new FileWriter(file);
            BufferedWriter bw = new BufferedWriter(fw);
            for(String line : data) {
                bw.write(line + "\n");
            }
            bw.close();

        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void captureImageFromSdCard() {
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");

            startActivityForResult(intent, SELECT_FILE);
    }

    public void captureImageFromCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //Uri fileUri = getOutputMediaFileUri(); // create a file to save the image
        //intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the image file name

        startActivityForResult(intent, TAKE_PICTURE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK)
            return;
        Uri imageUri = data.getData();
        switch (requestCode) {
            case TAKE_PICTURE:
                if(resultCode == RESULT_OK) {
                    if(user != null) {
                        if(user.get("isPremium").toString().equals("true")){
                            saveImageToParse(data);
                        }
                    }
                }
                imageFilePath = getRealPathFromURI(imageUri);
                break;
            case SELECT_FILE:
                imageFilePath = getRealPathFromURI(imageUri);
                if(user != null){
                    if(user.get("isPremium").toString().equals("true")) {
                        createImageFromPath(imageFilePath);
                    }
                }
                break;
        }

        new AsyncProcessTask(this).execute(imageFilePath, outputPath);
    }


    public String getRealPathFromURI(Uri imageUri)
    {
        try
        {
            String[] proj = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(imageUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return imageUri.getPath();
        }
    }

    private void createImageFromPath(String imageFilePath) {
        Bitmap imgBitMap = BitmapFactory.decodeFile(imageFilePath);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        imgBitMap.compress(Bitmap.CompressFormat.JPEG, 100, bos);

        byte[] scaledData = bos.toByteArray();

        saveImageToParse(scaledData);
    }

    private void saveImageToParse(byte[] scaledData) {
        ParseFile imgFile = new ParseFile("image.jpg", scaledData);

        ParseUser user = ParseUser.getCurrentUser();
        img = new ParseObject("Image");
        img.put("Image", imgFile);
        img.put("User", user);

        img.saveInBackground();
    }

    private void saveImageToParse(Intent data) {
        Bitmap mealImage = (Bitmap) data.getExtras().get("data");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        mealImage.compress(Bitmap.CompressFormat.JPEG, 100, bos);

        byte[] scaledData = bos.toByteArray();

        ParseFile photoFile = new ParseFile("image.jpg", scaledData);

        ParseUser user = ParseUser.getCurrentUser();
        img = new ParseObject("Image");
        img.put("Image", photoFile);
        img.put("User", user);

        img.saveInBackground();
    }

    public void updateResults(Boolean success) {
        if (!success)
            return;
        try {
            StringBuffer contents = new StringBuffer();
            List<String> receiptLines = new ArrayList();
            FileInputStream fis = openFileInput(outputPath);
            try {
                Reader reader = new InputStreamReader(fis, "UTF-8");
                BufferedReader bufReader = new BufferedReader(reader);
                String text = null;

                while ((text = bufReader.readLine()) != null) {
                    contents.append(text).append(System.getProperty("line.separator"));

                    //remove all spaces
                    text = text.replaceAll("\\s+", "");
                    //ignore lines that only contain whitespace and no characters
                    if(text.matches(".*\\w.*")) {
                        //this might seem redundant, but somehow the text variable still has leading and trailing spaces.
                        text = text.trim();
                        receiptLines.add(text);
                    }
                }
            } finally {
                fis.close();
            }

            processReceipt(null, receiptLines, false, true, "", false);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public byte[] getImageByte(String imageFilePath){
        try{
            File file = new File(imageFilePath);
            int size = (int) file.length();
            byte[] imageByte = new byte[size];
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            buf.read(imageByte, 0, imageByte.length);
            buf.close();
            return imageByte;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void processReceipt(byte[] imageIn, List<String> lines, boolean invokedFromStorage, boolean saveToStorage, String date, boolean queue){

        byte[] imageOut;
        //invoked after taking a picture
        if(imageIn == null && date.length() == 0 && imageFilePath != null) {
            imageOut = getImageByte(imageFilePath);
        }
        else{
            imageOut = imageIn;
        }
        String newDate;
        if(date.isEmpty()){
            newDate = getDate();
        }
        else{
            newDate = date.substring(0, date.length());
        }
        String groceryStore = "";
        String products = "";
        String prices = "";
        String subtotaal = "";
        boolean isFavorite = false;
        ArrayList<String> linesToFile = new ArrayList();

        boolean isGroceryStore = true;
        boolean isProduct = false;

        for(String line : lines){

            String _value = null;
            if(line.equals("isFavorite")){
                isFavorite = true;
                continue;
            }
            if(groceryStore.equals("")) {
                //when receipt data is loaded from abbyy api
                if(line.length() > 4) {
                    _value = line.substring(1, 5);
                }
                else{
                    //when receipt data is loaded from storage
                    _value = line;
                }
            }

            if(isGroceryStore) {
                switch (_value) {
                    case "Albe":
                        groceryStore = "AH";
                        linesToFile.add(groceryStore);
                        isGroceryStore = false;
                        break;
                    case "Aibe":
                        groceryStore = "AH";
                        linesToFile.add(groceryStore);
                        isGroceryStore = false;
                        break;
                    case "AH":
                        groceryStore = "AH";
                        linesToFile.add(groceryStore);
                        isGroceryStore = false;
                        break;
                    case "Jumb":
                        groceryStore = "Jumbo";
                        linesToFile.add(groceryStore);
                        isGroceryStore = false;
                        break;
                    case "Juib":
                        groceryStore = "Jumbo";
                        linesToFile.add(groceryStore);
                        isGroceryStore = false;
                        break;
                    case "umbo":
                        groceryStore = "Jumbo";
                        linesToFile.add(groceryStore);
                        isGroceryStore = false;
                        break;
                    case "Jumbo":
                        groceryStore = "Jumbo";
                        linesToFile.add(groceryStore);
                        isGroceryStore = false;
                        break;
                    default: continue;
                }
            }

            if(groceryStore.equals("AH") ||
                    groceryStore.equals("Jumbo")){

                //go to next item in the list if the line matches EUR. on the AH receipts products appear right underneath this line
                if(line.substring(0, line.length()).equals("EUR") ||
                        line.substring(0, line.length()).equals("AKKOORD")){
                    linesToFile.add(line);
                    isProduct = true;
                    continue;
                }
                if(isProduct) {
                    //only keep uppercase and lowercase letters
                    String _product = line.replaceAll("[^A-Za-z]", "") + "\n";
                    //retains the digits, dots and commas
                    String _price = line.replaceAll("[^\\d,.]+", " ") + "\n";
                    //replace all dots with commas
                    _price = _price.replace(".",",");

                    String _subTotaal = _product.trim();
                    linesToFile.add(line);
                    if (_subTotaal.equals("SUBTOTAAL") || _subTotaal.equals("TOTAAL")) {
                        subtotaal = _price;
                        isProduct = false;
                        break;
                    }else{
                        products += _product;
                        prices += "€"+ _price;
                    }
                }
            }
        }

        if(!invokedFromStorage) {
            if(saveToStorage) {
                saveDataToStorage(linesToFile);
            }
            if(user != null) {
                saveDataToCloud(groceryStore, products, prices, subtotaal, newDate, isFavorite, queue);
            }
            allFrag.addReceipt(allFrag.new ReceiptContent(imageOut, groceryStore, newDate, products, prices, subtotaal, isFavorite, "", imageFilePath));

        }
        else{
            if(isFavorite){
               favFrag.addReceipt(favFrag.new ReceiptContent(imageOut, groceryStore, date, products, prices, subtotaal, isFavorite, "", imageFilePath));
            }
               allFrag.addReceipt(allFrag.new ReceiptContent(imageOut, groceryStore, date, products, prices, subtotaal, isFavorite, "", imageFilePath));
        }
    }


    public void setImageFilePath(String path){
        imageFilePath = path;
    }
    private void saveDataToCloud(String store, String products, String prices, String subtotaal, String date, boolean isFavorite, boolean queue) {

        if(queue){
            img = new ParseObject("Image");
            img.put("User", user);

        }
        if(user.get("isPremium").toString().equals("true")){
            if(store.isEmpty() || products.isEmpty() || prices.isEmpty() || subtotaal.isEmpty()){
                Toast.makeText(MainActivity.this, "Cannot upload empty values!", Toast.LENGTH_SHORT).show();
            }else{
                if(img == null){
                    img = new ParseObject("Image");
                    img.put("User", user);
                }
                img.put("store", store);
                img.put("products", products);
                img.put("prices", prices);
                img.put("subtotaal", subtotaal);
                img.put("date", date);
                img.put("isFave", isFavorite);

                if(!queue) {
                    img.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e == null) {
                                Toast.makeText(MainActivity.this, "Premium: Receipt got uploaded to the cloud!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(MainActivity.this, "Failed to upload data, try again", Toast.LENGTH_SHORT).show();
                                e.printStackTrace();
                            }
                        }
                    });
                }else{
                    queuedForUpload.add(img);
                }
            }
        }else{
            Toast.makeText(MainActivity.this, "This is a premium feature", Toast.LENGTH_SHORT).show();
        }
    }

    public void saveAllToCloud(){
        try {
            ParseObject.saveAllInBackground(queuedForUpload, new SaveCallback() {
                @Override
                public void done(ParseException e) {

                    queuedForUpload.clear();
                    Toast.makeText(MainActivity.this, "All receipts have been uploaded!", Toast.LENGTH_SHORT).show();
                }
            });
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.example.gokhan.papurrless/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.example.gokhan.papurrless/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return ListFragment.FavFragment.newInstance(position + 1, premiumEnabled);
                case 1:
                    return ListFragment.AllFragment.newInstance(position + 1, premiumEnabled);
            }
            return null;
        }

        @Override
        public int getCount() {
            // Show 2 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.favorites_section);
                case 1:
                    return getString(R.string.all_section);
            }
            return null;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment createdFragment = (Fragment) super.instantiateItem(container, position);
            // save the appropriate reference depending on position
            switch (position) {
                case 0:
                    favFrag = (ListFragment.FavFragment) createdFragment;
                    if (allFrag != null) {
                        favFrag.setOtherFrag(allFrag);
                        allFrag.setOtherFrag(favFrag);
                    }
                    break;
                case 1:
                    allFrag = (ListFragment.AllFragment) createdFragment;
                    if (favFrag != null) {
                        allFrag.setOtherFrag(favFrag);
                        favFrag.setOtherFrag(allFrag);
                    }
                    break;
            }
            return createdFragment;
        }
    }

    public static boolean isOnline(Context context) //om te zien of er internetverbinding is
    {
        ConnectivityManager connectionManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectionManager == null) {
            return false;
        }
        NetworkInfo networkInfo = connectionManager.getActiveNetworkInfo();
        if (networkInfo == null) {
            return false;
        }
        return networkInfo.isConnected();
    }

}
