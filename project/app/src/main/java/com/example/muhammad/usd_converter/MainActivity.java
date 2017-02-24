package com.example.muhammad.usd_converter;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

    //Initialize Dollar variables
 /* int USD = 0;
    int GBP = 0;
    int EURO = 0;
    int YEN = 0;
    int REIS = 0;*/

    double num_of_billsUSD = 0;
    double num_of_billsOUT = 0;
    double exchangerateGBP = 0.79901;
    double exchangerateEURO = 0.94073;
    double exchangerateYEN = 114.54;
    double exchangerateREIS = 3.1343;

    EditText BillsField;
    TextView OutputField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BillsField = (EditText) findViewById(R.id.editText);
        OutputField = (TextView) findViewById(R.id.textView4);
    }





    void convertGBP(View view)
    {
        if(BillsField.getText().toString().equals(null) || BillsField.getText().toString().equals(""))
        {
            Toast.makeText(this,"Please enter an integer value", Toast.LENGTH_SHORT).show();
        }

        else
        {
            num_of_billsUSD = Double.parseDouble(BillsField.getText().toString());
            num_of_billsOUT = num_of_billsUSD * exchangerateGBP;

            OutputField.setText(num_of_billsOUT + " GBP");

            // EditText etUserName = (EditText) findViewById(R.id.txtUsername);
            // String strUserName = etUserName.getText().toString();
        }

    }

    void convertEURO(View view)
    {

        if(BillsField.getText().toString().equals(null) || BillsField.getText().toString().equals(""))
        {
            Toast.makeText(this,"Please enter an integer value", Toast.LENGTH_SHORT).show();
        }

        else
        {
            num_of_billsUSD = Double.parseDouble(BillsField.getText().toString());
            num_of_billsOUT = num_of_billsUSD * exchangerateEURO;

            OutputField.setText(num_of_billsOUT + " EUR");
        }
    }

    void convertYEN(View view)
    {

        if (BillsField.getText().toString().equals(null) || BillsField.getText().toString().equals(""))
        {
            Toast.makeText(this,"Please enter an integer value", Toast.LENGTH_SHORT).show();
        }

        else
        {
            num_of_billsUSD = Double.parseDouble(BillsField.getText().toString());
            num_of_billsOUT = num_of_billsUSD * exchangerateYEN;

            OutputField.setText(num_of_billsOUT + " JPY");
        }
    }

    void convertREIS(View view)
    {

        if (BillsField.getText().toString().equals(null) || BillsField.getText().toString().equals(""))
        {
            Toast.makeText(this,"Please enter an integer value", Toast.LENGTH_SHORT).show();
        }

        else
        {
            num_of_billsUSD = Double.parseDouble(BillsField.getText().toString());
            num_of_billsOUT = num_of_billsUSD * exchangerateREIS;

            OutputField.setText(num_of_billsOUT + " BRL");
        }
    }

}
