package com.boymask.edocs.passport;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.NfcF;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.boymask.edocs.CardData;
import com.boymask.edocs.R;
import com.boymask.edocs.Util;
import com.boymask.edocs.net.DataSender;
import com.google.android.material.snackbar.Snackbar;
import com.litetech.libs.restservicelib.RestService;

import org.jmrtd.BACKey;
import org.jmrtd.BACKeySpec;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;

public class PassportActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback, RestService.CallBack {
    private NfcAdapter mAdapter;
    private PendingIntent pendingIntent;
    private IntentFilter[] intentFiltersArray;
    private String[][] techListsArray;

    private TextView cognome;
    private TextView nome;
    private TextView codfiscale;
    private TextView datanascita;
    private TextView sesso;
    private TextView validafrom;
    private TextView validato;
    private TextView publish;
    private TextView stato;
    private TextView nazione;
    private TextView numdocumento;
    private ImageView imageView;
    private CardData data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passport);

        cognome = (TextView) findViewById(R.id.cognome);
        nome = (TextView) findViewById(R.id.nome);
        codfiscale = (TextView) findViewById(R.id.codfiscale);
        datanascita = (TextView) findViewById(R.id.datanascita);
        sesso = (TextView) findViewById(R.id.sesso);
        validafrom = (TextView) findViewById(R.id.validafrom);
        validato = (TextView) findViewById(R.id.validato);
        stato = (TextView) findViewById(R.id.stato);
        nazione = (TextView) findViewById(R.id.nazione);
        numdocumento = (TextView) findViewById(R.id.numdocumento);
        imageView = (ImageView) findViewById(R.id.imageView);

        publish = (TextView) findViewById(R.id.publish);
        Button ok = (Button) findViewById(R.id.ok);
        ok.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i("CMD", "Pressed");
                sendToServer();
            }
        });


        mAdapter = NfcAdapter.getDefaultAdapter(this);

        pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            ndef.addDataType("*/*");    /* Handles all MIME based dispatches.
                                       You should specify only the ones that you need. */
        } catch (IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException("fail", e);
        }
        intentFiltersArray = new IntentFilter[]{ndef};

        techListsArray = new String[][]{new String[]{NfcF.class.getName()}};


        Tag tag = getIntent().getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag == null) {
            return;
        }

    }

    private void sendToServer() {

        DataSender.send(data, this);

    }

    @Override
    public void onTagDiscovered(Tag tag) {
        if (Arrays.asList(tag.getTechList()).contains("android.nfc.tech.IsoDep")) {

            String passportNumber = "YB5889953";//preferences.getString(KEY_PASSPORT_NUMBER, null);
            String expirationDate = convertDate("2029-09-11");//convertDate(preferences.getString(KEY_EXPIRATION_DATE, null));
            String birthDate = convertDate("1980-10-05");//convertDate(preferences.getString(KEY_BIRTH_DATE, null));

            if (passportNumber != null && !passportNumber.isEmpty()
                    && expirationDate != null && !expirationDate.isEmpty()
                    && birthDate != null && !birthDate.isEmpty()) {
                BACKeySpec bacKey = new BACKey(passportNumber, birthDate, expirationDate);
                new ReadTask(PassportActivity.this, IsoDep.get(tag), bacKey).execute();

            } else {

                //    Snackbar.make(passportNumberView, R.string.error_input, Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    private static String convertDate(String input) {
        if (input == null) {
            return null;
        }
        try {
            return new SimpleDateFormat("yyMMdd", Locale.US)
                    .format(new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(input));
        } catch (ParseException e) {
            Log.w("WW", e);
            return null;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        NfcAdapter.getDefaultAdapter(this).disableReaderMode(this);
    }

    public void showResult(CardData data) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                cognome.setText(data.getCognome());
                nome.setText(data.getNome());
                codfiscale.setText(data.getCodFiscale());
                datanascita.setText(Util.convertDateToFull(data.getDataNascita()));
                sesso.setText(data.getSex());
                validafrom.setText(Util.convertDateToFull(data.getDataInizioValidita()));
                validato.setText(Util.convertDateToFull(data.getDataFineValidita()));
                stato.setText(data.getState());
                nazione.setText(data.getNationality());
                numdocumento.setText(data.getDocNumber());
                imageView.setImageBitmap(data.getPhoto());
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        Bundle options = new Bundle();
        options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 500);

        mAdapter.enableReaderMode(this, this,

                NfcAdapter.FLAG_READER_NFC_BARCODE |
                        NfcAdapter.FLAG_READER_NFC_A |
                        NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, options);
    }

    @Override
    public void onResult(String s, String s1) {

        System.out.println("onResult: "+s);
        System.out.println("onResult: "+s1);
    }
}