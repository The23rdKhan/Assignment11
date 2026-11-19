/**
 * Assignment 11
 * File Name: LoginFragment.java
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

import edu.charlotte.assignment11.databinding.FragmentLoginBinding;

/**
 * LoginFragment handles user authentication sign-in using Firebase Authentication.
 * Communicates success or navigation requests to MainActivity via LoginListener.
 */
public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private LoginListener mListener;

    public interface LoginListener {
        void onLoginSuccess();
        void onGoToSignUp();
    }

    public LoginFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof LoginListener) {
            mListener = (LoginListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement LoginListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Handle login button click
        binding.buttonLogin.setOnClickListener(v -> {
            String email = binding.editTextEmail.getText().toString().trim();
            String password = binding.editTextPassword.getText().toString().trim();

            if (email.isEmpty()) {
                Toast.makeText(getContext(), "Email is required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.isEmpty()) {
                Toast.makeText(getContext(), "Password is required", Toast.LENGTH_SHORT).show();
                return;
            }

            // Authenticate user with Firebase Auth
            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            if (mListener != null) {
                                mListener.onLoginSuccess();
                            }
                        } else {
                            String err = task.getException() != null ? task.getException().getMessage() : "Authentication failed";
                            Toast.makeText(getContext(), err, Toast.LENGTH_LONG).show();
                        }
                    });
        });

        // Navigate to Sign Up screen
        binding.buttonCreateAccount.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onGoToSignUp();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Prevent memory leaks with ViewBinding
    }
}
