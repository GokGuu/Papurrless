package com.example.gokhan.papurrless;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SignUpCallback;

public class RegisterActivity extends AppCompatActivity {

    EditText username;
    EditText password;
    EditText password2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        initViews();
    }

    public void registerUser(View view){
        String usr = username.getText().toString();
        String pwd = password.getText().toString();
        String pwd2 = password2.getText().toString();

        if(pwd.equals(pwd2)) {
            ParseUser user = new ParseUser();
            user.setUsername(usr);
            user.setPassword(pwd);

            user.signUpInBackground(new SignUpCallback() {
                public void done(ParseException e) {
                    if (e == null) {
                        Toast.makeText(RegisterActivity.this, "Account created!", Toast.LENGTH_SHORT).show();

                        Intent login = new Intent(RegisterActivity.this, LoginActivity.class);
                        startActivity(login);
                    } else {
                        Toast.makeText(RegisterActivity.this, "Something went wrong, please try again..", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }else{
            Toast.makeText(RegisterActivity.this, "Passwords don't match.", Toast.LENGTH_SHORT).show();
        }
    }

    public void initViews(){
        username = (EditText) findViewById(R.id.txt_username);
        password = (EditText) findViewById(R.id.txt_password);
        password2 = (EditText) findViewById(R.id.txt_password2);
    }
}
