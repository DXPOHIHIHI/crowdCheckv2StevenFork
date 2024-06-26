package com.example.customchu;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    Button btnToSignup;
    GoogleSignInOptions gso;
    GoogleSignInClient gsc;
    DatabaseReference DB;
    Profile userProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnToSignup = findViewById(R.id.btnToSignup);
        btnToSignup.setOnClickListener(v -> signIn());
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        gsc = GoogleSignIn.getClient(this, gso);

        DB = FirebaseDatabase.getInstance().getReference();
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);

        if (account != null) {
            if (!isUserAdamsonian(account)) {
                Toast.makeText(this, "Please use your Adamson email", Toast.LENGTH_SHORT).show();
                gsc.signOut();
                return;
            }

            Log.i("account id", account.getId());
            DatabaseReference ProfileReference = DB.child("Profiles").child(account.getId());
            ProfileReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        userProfile = snapshot.getValue(Profile.class);
                        Log.i("MainActivity", "onDataChange: " + userProfile);
                        toHomeActivity();
                    } else {
                        // notify the user to complete the profile
                        Toast.makeText(MainActivity.this, "Please complete your profile", Toast.LENGTH_SHORT).show();
                        toProfileActivity();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("MainActivity", "onCancelled", databaseError.toException());
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
//        if (account != null) {
//            toHomeActivity();
//        }
    }

    private void signIn() {
        Intent intent = gsc.getSignInIntent();
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount gmsacc = task.getResult(ApiException.class);
                if (!isUserAdamsonian(gmsacc)) {
                    Toast.makeText(this, "Please use your Adamson email", Toast.LENGTH_SHORT).show();
                    gsc.signOut();
                    return;
                }
                toHomeActivity();
            } catch (ApiException e) {
                Log.e("GoogleSignIn", "Error signing in: " + e.getStatusCode());
                Toast.makeText(this, "ERROR: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();

                switch (e.getStatusCode()) {
                    case 7:  // NETWORK_ERROR
                        Toast.makeText(this, "Network error. Please check your connection.", Toast.LENGTH_SHORT).show();
                        break;
                    case 10: // DEVELOPER_ERROR
                        Toast.makeText(this, "Developer error. Please check your configuration.", Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        Toast.makeText(this, "Unknown error occurred. Please try again.", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }
    }


    private boolean isUserAdamsonian(GoogleSignInAccount gmsacc) {
        String email = Objects.requireNonNull(gmsacc.getEmail());
        return email.endsWith("@adamson.edu.ph") || email.endsWith("@gmail.com");
        //return Objects.requireNonNull(gmsacc.getEmail()).contains("@adamson.edu.ph");

    }

    private void toProfileActivity() {
        Intent intent = new Intent(getApplicationContext(), profileActivity.class);
        intent.putExtra("profile", userProfile);
        startActivity(intent);
    }

    private void toHomeActivity() {
        Intent intent = new Intent(getApplicationContext(), home.class);
        intent.putExtra("profile", userProfile);
        startActivity(intent);
    }
}


