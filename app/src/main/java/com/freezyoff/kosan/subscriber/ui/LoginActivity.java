package com.freezyoff.kosan.subscriber.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;

import com.freezyoff.kosan.subscriber.R;
import com.freezyoff.kosan.subscriber.model.User;
import com.freezyoff.kosan.subscriber.ui.main.UserViewModel;

public class LoginActivity extends AppCompatActivity {

    UserViewModel userModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        userModel = ViewModelProviders.of(this).get(UserViewModel.class);

        if (hasSavedUser()){
            redirectDashboardActivity(userModel.getUser().getValue());
        }
        else{
            prepareView();
        }
    }

    private boolean hasSavedUser(){
        return userModel.getUser().getValue() != null;
    }

    private void prepareView(){
        Button button = findViewById(R.id.btLogin);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText txEmail = findViewById(R.id.txEmail);
                EditText txPassword = findViewById(R.id.txPassword);
                redirectDashboardActivity(
                        new User(
                                txEmail.getText().toString(),
                                txPassword.getText().toString()
                        )
                );
            }
        });
    }

    private void redirectDashboardActivity(User user){

        userModel.getUser().setValue(user);
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}
