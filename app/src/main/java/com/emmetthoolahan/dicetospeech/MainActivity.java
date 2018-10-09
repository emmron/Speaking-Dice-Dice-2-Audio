package com.emmetthoolahan.dicetospeech;

import android.content.Context;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.emmetthoolahan.dicetospeech.*;

import java.util.Locale;

import  emmetthoolahan.dicetospeech.R;

public class MainActivity extends AppCompatActivity {

    TextToSpeech tts;
    TextView setDiceSidesTextView;
    String amountOfSides;
    Boolean amountOfSidesSet = false;
    int theRandomNumber;
    int amountOfSidesInt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeDiceTextField();
    }

    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        View v = super.onCreateView(name, context, attrs);
        return v;
    }

    private void initializeDiceTextField() {
        setDiceSidesTextView = (TextView) findViewById(R.id.egh_set_sides);
    }

    public void rollTheDice(View view) {
        System.out.println("rolling the dice....");
        if (amountOfSidesSet) {
            String amountOfSidesDynamic = Integer.toString(amountOfSidesInt);
            System.out.println("the amount of sides is: " + amountOfSidesDynamic);
            theRandomNumber = getRandomDoubleBetweenRange(1, amountOfSidesInt);
            System.out.println("the amount of sides isrrrr: " + String.valueOf(theRandomNumber));

        } else {
            theRandomNumber = getRandomDoubleBetweenRange(1, 6);
        }
        //String numberToReadOut = getStringForNumber(theRandomNumber);
        String numberToSay = String.valueOf(theRandomNumber);

        readOutTheNumber(numberToSay);
        System.out.println("the random number is.." + String.valueOf(theRandomNumber));
    }

    public String getStringForNumber(int diceNumber) {
        String diceNumberString;
        switch (diceNumber) {
            case 1:
                diceNumberString = "One";
                break;
            case 2:
                diceNumberString = "Two";
                break;
            case 3:
                diceNumberString = "Three";
                break;
            case 4:
                diceNumberString = "Four";
                break;
            case 5:
                diceNumberString = "Five";
                break;
            case 6:
                diceNumberString = "Six";
                break;
            default:
                diceNumberString = "Error";
                break;
        }
        return diceNumberString;
    }

    public void readOutTheNumber(final String number_to_read) {
         tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                // TODO Auto-generated method stub
                if(status == TextToSpeech.SUCCESS){
                    int result = tts.setLanguage(Locale.US);
                    if(result==TextToSpeech.LANG_MISSING_DATA ||
                            result==TextToSpeech.LANG_NOT_SUPPORTED){
                        Log.e("error", "This Language is not supported");
                    }
                    else{
                        ConvertTextToSpeech(number_to_read);
                    }
                }
                else
                    Log.e("error", "Initilization Failed!");
            }
        });
    }

    private void ConvertTextToSpeech(String toBeSpoken) {
        tts.speak(toBeSpoken, TextToSpeech.QUEUE_FLUSH, null);
    }

    public static int getRandomDoubleBetweenRange(double min, double max){
        double x = (Math.random()*((max-min)+1))+min;
        int i = (int) x;
        return i;
    }

    public void setTheAmountOfDiceSides(View view) {
        amountOfSides = String.valueOf(setDiceSidesTextView.getText());
        String amountOfSidesTextToSpeech = "Amount of sides has been set to: " + amountOfSides;
        amountOfSidesSet = true;
        amountOfSidesInt = Integer.valueOf(amountOfSides);
        readOutTheNumber(amountOfSidesTextToSpeech);
    }
}
