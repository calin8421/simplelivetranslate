package com.gheorghecalin.simplelivetranslate;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.MotionEvent;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private Spinner languageSpinner;
    private TextView outputText;
    private Button speakButton;
    private Button speakOutButton;
    private TextToSpeech textToSpeech;

    private String originalText = "";
    private String translatedText = "";

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        languageSpinner = findViewById(R.id.languageSpinner);
        outputText = findViewById(R.id.outputText);
        speakButton = findViewById(R.id.speakButton);
        speakOutButton = findViewById(R.id.speakOutButton);

        setupSpinner();
        setupTextToSpeech();
        setupSpeakButton();
        setupSpeakOutButton();
    }

    private void setupSpinner() {
        String[] languages = {"English", "Romanian", "Spanish", "French", "German", "Italian", "Japanese", "Korean"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                languages
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(adapter);
    }

    private void setupTextToSpeech() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.US);
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupSpeakButton() {
        speakButton.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    speakButton.setText(R.string.LISTEN);
                    startRecording();
                    return true;

                case MotionEvent.ACTION_UP:
                    v.performClick();
                    speakButton.setText(R.string.speak);
                    stopRecordingAndTranslate();
                    return true;

                case MotionEvent.ACTION_CANCEL:
                    speakButton.setText(R.string.speak);
                    stopRecordingAndTranslate();
                    return true;
                default:
                    return false;
            }

        });
    }

    private void setupSpeakOutButton() {
        speakOutButton.setOnClickListener(v -> {
            if (translatedText != null && !translatedText.isEmpty()) {
                setTtsLanguage();
                textToSpeech.speak(translatedText, TextToSpeech.QUEUE_FLUSH, null, "translation");
            }
        });
    }

    private void startRecording() {
        originalText = "";
        translatedText = "";
        outputText.setText(R.string.listen);
        // start microphone recording here
        // or start speech recognizer here
    }


    private void stopRecordingAndTranslate() {
        outputText.setText(R.string.translating);

        // Temporary example:
        originalText = "Bună ziua, ce mai faci?";
        translatedText = "Hello, how are you?";

        outputText.setText(getString(R.string.input_translation_format,originalText,translatedText));
        // stop recording
        // send audio/text to backend
        // get translation result
    }

    private void setTtsLanguage() {
        String selected = languageSpinner.getSelectedItem().toString();

        switch (selected) {
            case "Romanian":
                textToSpeech.setLanguage(new Locale("ro", "RO"));
                break;
            case "Spanish":
                textToSpeech.setLanguage(new Locale("es", "ES"));
                break;
            case "French":
                textToSpeech.setLanguage(Locale.FRANCE);
                break;
            case "German":
                textToSpeech.setLanguage(Locale.GERMANY);
                break;
            case "Italian":
                textToSpeech.setLanguage(Locale.ITALY);
                break;
            case "Japanese":
                textToSpeech.setLanguage(Locale.JAPAN);
                break;
            case "Korean":
                textToSpeech.setLanguage(Locale.KOREA);
                break;
            default:
                textToSpeech.setLanguage(Locale.US);
                break;
        }
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}