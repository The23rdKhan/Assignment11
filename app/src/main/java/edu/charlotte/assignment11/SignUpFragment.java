/**
 * Assignment 11
 * File Name: SignUpFragment.java
 * Full Name of Student: Lucnel Nordelus
 */
package edu.charlotte.assignment11;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import edu.charlotte.assignment11.databinding.FragmentSignupBinding;

public class SignUpFragment extends Fragment {

    private FragmentSignupBinding binding;
    private SignUpListener mListener;

    public interface SignUpListener {
        void onSignUpSuccess();
        void onCancelSignUp();
    }

    public SignUpFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof SignUpListener) {
            mListener = (SignUpListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement SignUpListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSignupBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonRegister.setOnClickListener(v -> {
            String name = binding.editTextName.getText() != null ? binding.editTextName.getText().toString().trim() : "";
            String email = binding.editTextEmail.getText() != null ? binding.editTextEmail.getText().toString().trim() : "";
            String password = binding.editTextPassword.getText() != null ? binding.editTextPassword.getText().toString().trim() : "";

            if (name.isEmpty()) {
                Toast.makeText(getContext(), "Name is required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (email.isEmpty()) {
                Toast.makeText(getContext(), "Email is required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(getContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            if (user != null) {
                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(name)
                                        .build();
                                user.updateProfile(profileUpdates).addOnCompleteListener(updateTask -> {
                                    if (mListener != null) {
                                        mListener.onSignUpSuccess();
                                    }
                                });
                            } else {
                                if (mListener != null) {
                                    mListener.onSignUpSuccess();
                                }
                            }
                        } else {
                            String err = task.getException() != null ? task.getException().getMessage() : "Registration failed";
                            Toast.makeText(getContext(), err, Toast.LENGTH_LONG).show();
                        }
                    });
        });

        binding.buttonCancel.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onCancelSignUp();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}