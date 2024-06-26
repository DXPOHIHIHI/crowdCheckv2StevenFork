package com.example.customchu;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;

public class profileActivity extends AppCompatActivity {
    ImageButton profileBack;
    ImageView profilePicture;
    EditText firstName, studentNumber, email, uid;
    Button logout, changeInfo;
    GoogleSignInOptions gso;
    GoogleSignInClient gsc;
    DatabaseReference DB;
    Profile profile;
    ImageButton copytoclipboard;

    private void copyTextToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("ID Number", text);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile);

        // create a reference to the database
        DB = FirebaseDatabase.getInstance().getReference();
        profile = getIntent().getSerializableExtra("profile") != null ? (Profile) getIntent().getSerializableExtra("profile") : new Profile();
        Log.i("profilePage", profile.toString());

        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        gsc = GoogleSignIn.getClient(this, gso);

        profileBack = findViewById(R.id.profileBack);
        profilePicture = findViewById(R.id.profilepicture);
        firstName = findViewById(R.id.firstname);
        studentNumber = findViewById(R.id.studentNumber);
        email = findViewById(R.id.email);
        logout = findViewById(R.id.toLogout);
        changeInfo = findViewById(R.id.changeinfo);
        uid = findViewById(R.id.idnumber);

        /// BRUH
        copytoclipboard = findViewById(R.id.copytoclipboard);
        copytoclipboard.setOnClickListener(view -> copyTextToClipboard(uid.getText().toString()));




        profileBack.setOnClickListener(view -> {
            Intent intent = new Intent(profileActivity.this, home.class);
            intent.putExtra("profile", profile);
            startActivity(intent);
        });
        logout.setOnClickListener(view -> gsc.signOut().addOnCompleteListener(this, task -> {
            // navigate back to home activity
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }));
        changeInfo.setOnClickListener(view -> updateProfile());

        updateUserinfoUI();








    }

    private void updateUserinfoUI() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        // update the profile picture regardless of the account status
        String profilePicUrl = account != null && account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : "";
        if (!profilePicUrl.equals("")) {
            Picasso.get()
                    .load(profilePicUrl)
                    .into(profilePicture);
        }

        // check if the account exists and update other UI fields
        if (account != null) {
            email.setText(account.getEmail());
            firstName.setText(account.getGivenName() + " " + account.getFamilyName());

            // Fetch student number and UID from the database
            DatabaseReference ProfileReference = DB.child("Profiles").child(account.getId());
            ProfileReference.child("student_id").get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Integer studentIdValue = task.getResult().getValue(Integer.class);
                    if (studentIdValue != null) {
                        studentNumber.setText(String.valueOf(studentIdValue));
                    }
                } else {
                    Log.e("TAG", "Error getting student ID", task.getException());
                }
            });

            ProfileReference.child("uid").get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Integer uidValue = task.getResult().getValue(Integer.class);
                    if (uidValue != null) {
                        uid.setText(String.valueOf(uidValue));
                    }
                } else {
                    Log.e("TAG", "Error getting UID", task.getException());
                }
            });

            // make the fields read-only
            email.setEnabled(false);
            firstName.setEnabled(false);
        }
    }

    private void updateProfile() {
        try {
            // Fetch the student number and UID from the UI
            String studentNumber = this.studentNumber.getText().toString();
            String userId = uid.getText().toString();

            // Check if the student number or UID is empty
            if (studentNumber.isEmpty() || userId.isEmpty()) {
                Toast.makeText(this, "Please complete your profile", Toast.LENGTH_SHORT).show();
                return;
            }

            // Fetch the currently signed-in account
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
            if (account == null) {
                Toast.makeText(this, "User is not signed in", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create a reference to the profile
            DatabaseReference profileReference = DB.child("Profiles").child(account.getId());

            // Create a new profile object
            profile = new Profile(Integer.parseInt(studentNumber));

            // Set the value of the profile
            profileReference.setValue(profile).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Set the UID in the profile
                    DatabaseReference userIdReference = profileReference.child("uid");
                    userIdReference.setValue(Integer.parseInt(userId)).addOnCompleteListener(task1 -> {
                        if (task1.isSuccessful()) {
                            // Get the name of the user
                            String userName = account.getGivenName() + " " + account.getFamilyName();

                            // Update the user's name in the users node
                            DatabaseReference nameReference = DB.child("users").child(userId).child("student_name");
                            nameReference.setValue(userName).addOnCompleteListener(task2 -> {
                                if (task2.isSuccessful()) {
                                    Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                                } else {
                                    Log.e("ProfileUpdate", "Error updating user name", task2.getException());
                                    Toast.makeText(this, "Error updating user name", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            Log.e("ProfileUpdate", "Error updating UID", task1.getException());
                            Toast.makeText(this, "Error updating UID", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Log.e("ProfileUpdate", "Error updating profile", task.getException());
                    Toast.makeText(this, "Error updating profile", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e("ProfileUpdate", "Exception during profile update", e);
            Toast.makeText(this, "There has been an error. Profile has not been updated", Toast.LENGTH_SHORT).show();
        }
    }


    // Method to copy text to clipboard










}

