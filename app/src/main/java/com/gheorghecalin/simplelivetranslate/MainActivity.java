package com.gheorghecalin.simplelivetranslate;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.MotionEvent;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;


import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;

import java.util.ArrayList;
import java.util.Locale;



public class MainActivity extends AppCompatActivity {

    private Spinner languageSpinner;
    private TextView outputText;
    private Button speakButton;
    private Button speakOutButton;
    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;
    private TextToSpeech textToSpeech;

    private String recognizedText = "";
    private String translatedText = "";

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if(!isGranted) {
                    Toast.makeText(this, "Microphone permission is required", Toast.LENGTH_LONG).show();
                }
            });

    private void checkMicPermission(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

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
        checkMicPermission();
        setupSpeechRecognizer();
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



    private void setupSpeechRecognizer() {
        if(!SpeechRecognizer.isRecognitionAvailable(this)){
            outputText.setText("Speech recognition is not available on this device.");
            return;
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        if(Build.VERSION.SDK_INT >= 34) {
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_ENABLE_LANGUAGE_DETECTION, true);
            ArrayList<String> allowedLanguages = new ArrayList<>();
            allowedLanguages.add("en-US");
            allowedLanguages.add("ro-RO");
            allowedLanguages.add("es-ES");
            allowedLanguages.add("fr-FR");
            allowedLanguages.add("de-DE");
            allowedLanguages.add("it-IT");
            allowedLanguages.add("ja-JP");
            allowedLanguages.add("ko-KR");
            recognizerIntent.putStringArrayListExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_DETECTION_ALLOWED_LANGUAGES, allowedLanguages
            );
        }

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onBeginningOfSpeech() {

            }

            @Override
            public void onBufferReceived(byte[] bytes) {

            }

            @Override
            public void onEndOfSpeech() {
                outputText.setText(R.string.translating);
            }

            @Override
            public void onError(int errorIndex) {
                outputText.setText("Speech recognition error: " + errorIndex);
            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if(matches != null && !matches.isEmpty()){
                    outputText.setText("Input:\n"+matches.get(0)+"\n\nTranslation:\n...");
                }
            }

            @Override
            public void onReadyForSpeech(Bundle bundle) {
                outputText.setText(R.string.listen);
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if(matches != null && !matches.isEmpty()){
                    recognizedText = matches.get(0);
                    detectLanguageAndTranslate(recognizedText);
                } else {
                    outputText.setText("No speech recognized.");
                }
            }

            @Override
            public void onRmsChanged(float v) {

            }
        });
    }
    private void setupTextToSpeech() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.SUCCESS) {
                outputText.setText("TTS init failed.");
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupSpeakButton() {
        speakButton.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    speakButton.setText(R.string.LISTEN);
                    recognizedText = "";
                    translatedText = "";
                    outputText.setText(R.string.listen);
                    if(speechRecognizer != null){
                        speechRecognizer.startListening(recognizerIntent);
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                    v.performClick();
                    speakButton.setText(R.string.speak);
                    if(speechRecognizer != null){
                        speechRecognizer.stopListening();
                    }
                    return true;

                case MotionEvent.ACTION_CANCEL:
                    speakButton.setText(R.string.speak);
                    if(speechRecognizer != null) {
                        speechRecognizer.stopListening();
                    }
                    return true;
                default:
                    return false;
            }


        });
    }

    private void setupSpeakOutButton() {
        speakOutButton.setOnClickListener(v -> {
            if (!translatedText.isEmpty()) {
                if(setTtsLanguage()){
                    textToSpeech.speak(translatedText, TextToSpeech.QUEUE_FLUSH, null, "translated_text");
                }
            }
        });
    }

    private String getTargetLanguageCode(String languageName) {
        switch (languageName){
            case "Romanian":
                return TranslateLanguage.ROMANIAN;
            case "Spanish":
                return TranslateLanguage.SPANISH;
            case "French":
                return TranslateLanguage.FRENCH;
            case "German":
                return TranslateLanguage.GERMAN;
            case "Italian":
                return TranslateLanguage.ITALIAN;
            case "Japanese":
                return TranslateLanguage.JAPANESE;
            case "Korean":
                return TranslateLanguage.KOREAN;
            default:
                return TranslateLanguage.ENGLISH;
        }
    }

    private void translateRecognizedText(String inputText, String detectedLanguageCode){
        String sourceCode = TranslateLanguage.fromLanguageTag(detectedLanguageCode);
        String targetCode = getTargetLanguageCode(languageSpinner.getSelectedItem().toString());
        if(sourceCode == null) {
            outputText.setText("Detected language is not supported for translation: " + detectedLanguageCode);
            return;
        }
        if(sourceCode.equals(targetCode)) {
            translatedText = inputText;
            outputText.setText("Input:\n" + inputText + "\n\nTranslation:\n" + translatedText);
            return;
        }
        TranslatorOptions options = new TranslatorOptions.Builder().setSourceLanguage(sourceCode).setTargetLanguage(targetCode).build();
        Translator translator = Translation.getClient(options);
        DownloadConditions conditions = new DownloadConditions.Builder().build();
        translator.downloadModelIfNeeded(conditions).addOnSuccessListener(unused ->
                        translator.translate(inputText)
                                .addOnSuccessListener(result -> {
                                    translatedText = result;
                                    outputText.setText(
                                            "Detected: " + detectedLanguageCode +
                                                    "\n\nInput:\n" + inputText +
                                                    "\n\nTranslation:\n" + translatedText
                                    );
                                    translator.close();
                                })
                                .addOnFailureListener(e -> {
                                    outputText.setText("Translation failed: " + e.getMessage());
                                    translator.close();
                                })
                )
                .addOnFailureListener(e -> {
                    outputText.setText("Model download failed: " + e.getMessage());
                    translator.close();
                });
    }

    private void detectLanguageAndTranslate(String inputText) {
        LanguageIdentifier languageIdentifier = LanguageIdentification.getClient();
        languageIdentifier.identifyLanguage(inputText).addOnSuccessListener(languageCode -> {
            if(languageCode == null || languageCode.equals("und")) {
                outputText.setText("Could not detect language");
                languageIdentifier.close();
                return;
            }
            translateRecognizedText(inputText, languageCode);
            languageIdentifier.close();
        })
                .addOnFailureListener(e->{
                    outputText.setText("Language detection failed: " +e.getMessage());
                    languageIdentifier.close();
                });
    }

    private boolean setTtsLanguage() {
        String selected = languageSpinner.getSelectedItem().toString();
        Locale targetLocale;
        switch (selected) {
            case "Romanian":
                targetLocale = new Locale("ro", "RO");
                break;
            case "Spanish":
                targetLocale = new Locale("es", "ES");
                break;
            case "French":
                targetLocale = Locale.FRANCE;
                break;
            case "German":
                targetLocale = Locale.GERMANY;
                break;
            case "Italian":
                targetLocale = Locale.ITALY;
                break;
            case "Japanese":
                targetLocale = Locale.JAPAN;
                break;
            case "Korean":
                targetLocale = Locale.KOREA;
                break;
            default:
                targetLocale = Locale.US;
                break;
        }
        int availability = textToSpeech.isLanguageAvailable(targetLocale);
        if(availability == TextToSpeech.LANG_MISSING_DATA || availability == TextToSpeech.LANG_NOT_SUPPORTED){
            outputText.setText("TTS language not available on this device: " + selected);
            return false;
        }
        int result = textToSpeech.setLanguage(targetLocale);
        if(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED){
            outputText.setText("Could not set TTS language: " + selected);
            return false;
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}