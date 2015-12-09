package com.example.rickstyinc.nfcid;

import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.nfc.NfcAdapter;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**TODO :
 *NOT DOABLE Activation des capteurs Bluetooth et NOT POSSIBLE - du réseau 3G/cellulaire
 *DONE -*Programmation du comportement via IHM
 *DONE Lancement d'une app
 * DONE - Enregistrement dynamique des ID
 * DONE -foreground thingy
 * Ranger les fonctions
 * DONE+ : uniquement vertical
 *MIGHT NOT BE DOABLE toggle localisation
 * gestion des différents volumes
 * implémentation bouton "Annuler"
 * modifier la mise en page (utiliser des sous-layouts
 * + : mettre le form dans un layout et toggle sa visibilité**/


public class MainActivity extends AppCompatActivity {

    private final String[][] techList = new String[][] {
            new String[] {
                    NfcA.class.getName(),
                    NfcB.class.getName(),
                    NfcF.class.getName(),
                    NfcV.class.getName(),
                    IsoDep.class.getName(),
                    MifareClassic.class.getName(),
                    MifareUltralight.class.getName(), Ndef.class.getName()
            }
    };

    private TextView monTexte;
    private String nfc_univCard = null;
    private boolean histVisible = false; /*L'historique est-il visible?*/

    //Classe permettant d'intéragir avec le module NFC de l'appareil
    private NfcAdapter mNfcAdapter;
    //Classe permettant d'agir sur la configuration du Wi-Fi de l'appareil
    private WifiManager wifiMan;
    //Classe permettant de modifier le/les volumes sonore(s) dans un certain contexte
    private AudioManager audioManager;
    //Classe permettant de modifier l'état du Bluetooth
    private BluetoothAdapter btAdapter;

    //ID lu par le module NFC
    private String detectedId;

    //Utilisé pour montrer/cacher le clavier
    private InputMethodManager imm;

//Enregistrement de données :

    //Méthode 2: Utilisation de la classe sharedPreferences :
    private SharedPreferences sharedPrefId;  /*nom : preferenceCleId*/
    private SharedPreferences setWifi;  /*nom : preferenceIdWifi*/
    private SharedPreferences setValWifi;  /*nom : preferenceIdValWifi*/
    private SharedPreferences setMute;
    private SharedPreferences setValMute;
    private SharedPreferences setVolume;    /*nom : preferenceIdVol; vol=0=>pas de modifs*/
    private SharedPreferences setBluetooth; /*nom : preferenceIdBt; true=>agit sur le Bt*/
    private SharedPreferences valBt; /*nom : preferenceIdValBt; true=>allume-false=>eteint*/
    private SharedPreferences setApp; /*nom : preferenceIDApp; true=>agit lance une app*/
    private SharedPreferences appChoisie; /*nom : preferenceIdAppCh; retourne nom App*/

    private Map<String, ?> allPreferences;
    private Map<String, ?> allPrefWifi;

    //Boite d'entree de nom de tag
    private EditText tagNamingBox;
    private String nomDonne;

    //Paramétrages :
    private CheckBox activWifi;
    private Switch switchWifi;
    private CheckBox activMute;
    private Switch switchMute;
    private CheckBox activVol;
    private Spinner volSpinner;
    private CheckBox activBluetooth;
    private Switch switchBluetooth;
    private CheckBox activApp;
    private Spinner spinnerApp;


    //Bouton valider
    private Button boutonValider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //On rend nos paramètres invisible
        cacherParametrage();

        //Creation de notre boite de texte
        monTexte = (TextView) findViewById(R.id.monTexte);

        //Detection du NFC
        mNfcAdapter =  NfcAdapter.getDefaultAdapter(this);

        //On détecte si l'appareil à un capteur NFC, et si celui-ci est activé
        if(mNfcAdapter == null){
            monTexte.setText("Cet appareil n'a pas de capteur NFC !!");
        }
        else{
            if(!mNfcAdapter.isEnabled()){
                monTexte.setText("Il faut activer le capteur NFC !!");
                //TODO : toggle NFC!!!
            }
            else
                monTexte.setText(R.string.defaultMessage);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        // creating pending intent (autorise d'autres app à utiliser notre intent avec notre niveau de permissions):
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        // creating intent receiver for NFC events:
        IntentFilter filter = new IntentFilter();
        filter.addAction(NfcAdapter.ACTION_TAG_DISCOVERED);
        filter.addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filter.addAction(NfcAdapter.ACTION_TECH_DISCOVERED);
        // enabling foreground dispatch for getting intent from NFC event:
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, new IntentFilter[]{filter}, this.techList);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //Code V2
        // disabling foreground dispatch:
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {

        //On code le comportement lorsqu'un TAG NFC est détecté
        if (intent.getAction().equals(NfcAdapter.ACTION_TAG_DISCOVERED)) {
            detectedId = ByteArrayToHexString(intent.getByteArrayExtra(NfcAdapter.EXTRA_ID));
            monTexte.setText(
                    "NFC Tag\n" +
                            detectedId);
            sharedPrefId = this.getSharedPreferences("preferenceCleId", Context.MODE_PRIVATE);
            allPreferences = sharedPrefId.getAll();
            setWifi = this.getSharedPreferences("preferenceIdWifi", Context.MODE_PRIVATE);
            setValWifi = this.getSharedPreferences(getString(R.string.preferenceIdValWifi), Context.MODE_PRIVATE);
            setVolume = this.getSharedPreferences(getString(R.string.preferenceIdVol), Context.MODE_PRIVATE);
            setMute = this.getSharedPreferences("preferenceIdMute", Context.MODE_PRIVATE);
            setValMute = this.getSharedPreferences(getString(R.string.preferenceIdValMute), Context.MODE_PRIVATE);
            setBluetooth = this.getSharedPreferences(getString(R.string.preferenceIdBt), Context.MODE_PRIVATE);
            valBt = this.getSharedPreferences(getString(R.string.preferenceIdValBt), Context.MODE_PRIVATE);
            setApp = this.getSharedPreferences("preferenceIdApp", Context.MODE_PRIVATE) ;
            appChoisie = this.getSharedPreferences("preferenceIdAppCh", MODE_PRIVATE) ;


            //if(Objects.equals(detectedId, sharedPrefId.getString("nfc_univCard", null))){
            if(allPreferences.containsKey(detectedId)){
                String nomTag = sharedPrefId.getString(detectedId, "INCONNU");
                monTexte.setText("Salut "+nomTag+"!!");
                Toast.makeText(this, "Tag reconnu: Hi "+nomTag+"!!", Toast.LENGTH_SHORT).show();

                //Si le tag détécté gère le wifi:
                if (setWifi.getBoolean(detectedId, false))
                    interrupteurWifi(setValWifi.getBoolean(detectedId, false));

                //Si le tag détécté gère le mode silencieux:
                if (setMute.getBoolean(detectedId, false))
                    interrupteurSon(setValMute.getBoolean(detectedId, false));

                //Si le tag détécté gère la modification du volume
                int volDetecte = setVolume.getInt(detectedId,0);
                if (volDetecte>0) {
                    choisirVolume(volDetecte);
                }

                //Si le tag gère la modification du Bluetooth
                if (setBluetooth.getBoolean(detectedId, false))
                    allumeEteintBt(valBt.getBoolean(detectedId, false));

                //Si le tag gère le lancement d'une app
                if (setApp.getBoolean(detectedId, false)){
                    CharSequence nomAppC = (CharSequence)appChoisie.getString(detectedId, null);
                    lanceApp(nomAppC);
                    Toast.makeText(this, "Ouverture de l'application "+nomAppC, Toast.LENGTH_LONG).show();
                }
            }

            else{
                monTexte.setText("Tag non reconu; veuillez lui donner un nom");
                afficherParametrage();

                //Lorsque l'on valide notre entrée, on enregistre le tag, avec pour clé le texte entré
                tagNamingBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        boolean handled = false;
                        if (actionId == EditorInfo.IME_ACTION_SEND) {
                            imm.hideSoftInputFromWindow(monTexte.getWindowToken(), 0);
                            handled = true;
                        }
                        return handled;
                    }
                });
            }
            }
        }


    // Converting byte[] to hex string (conversion de l'id du tag reçu):
    private String ByteArrayToHexString(byte [] inarray) {
        int i, j, in;
        String [] hex = {"0","1","2","3","4","5","6","7","8","9","A","B","C","D","E","F"};
        String out= "";
        for(j = 0 ; j < inarray.length ; ++j)
        {
            in = (int) inarray[j] & 0xff;
            i = (in >> 4) & 0x0f;
            out += hex[i];
            i = in & 0x0f;
            out += hex[i];
        }
        return out;
    }

    private void ajouteEntreeStringSP(String cle, String val, SharedPreferences SP){
        SharedPreferences.Editor edito = SP.edit();
        edito.putString(cle, val);
        edito.commit();
        //Toast.makeText(this, "L'entrée "+cle+" de valeur "+val+" a été ajoutée.", Toast.LENGTH_LONG).show();
    }

    private void ajouteEntreeBoolSP(String cle, boolean val, SharedPreferences SP){
        SharedPreferences.Editor edito = SP.edit();
        edito.putBoolean(cle, val);
        edito.commit();
    }

    private void ajouteEntreeIntSP(String cle, int val, SharedPreferences SP){
        SharedPreferences.Editor edito = SP.edit();
        edito.putInt(cle, val);
        edito.commit();
    }

    private String getKey(Map<String, ?> spAll, String val){
        String res ="rien trouvé";
        for(Map.Entry<String, ?> entree : spAll.entrySet()){
            String valTrouvee = (String) entree.getValue();
            if (val.equals(valTrouvee)) {
                res = entree.getKey();
            }
        }
        return res;
    }

    //Affichage de l'historique
    private void showAllPreferences(TextView tv, Map<String, ?> spAll) {

        if (histVisible) {
            tv.setText(R.string.defaultMessage);
            histVisible = !histVisible;
        }
        else {
            String texte = "Valeurs enregistrées : \n";
            setWifi = this.getSharedPreferences("preferenceIdWifi", Context.MODE_PRIVATE);
            setValWifi = this.getSharedPreferences(getString(R.string.preferenceIdValWifi), Context.MODE_PRIVATE);
            setMute = this.getSharedPreferences("preferenceIdMute", Context.MODE_PRIVATE);
            setValMute = this.getSharedPreferences(getString(R.string.preferenceIdValMute), Context.MODE_PRIVATE);
            setVolume = this.getSharedPreferences(getString(R.string.preferenceIdVol), Context.MODE_PRIVATE);
            setBluetooth = this.getSharedPreferences(getString(R.string.preferenceIdBt), Context.MODE_PRIVATE);
            valBt = this.getSharedPreferences(getString(R.string.preferenceIdValBt), Context.MODE_PRIVATE);
            setApp = this.getSharedPreferences("preferenceIdApp", Context.MODE_PRIVATE) ;
            appChoisie = this.getSharedPreferences("preferenceIdAppCh", MODE_PRIVATE) ;
            for (Map.Entry<String, ?> entree : spAll.entrySet()) {
                String cle = entree.getKey();
                Object val = entree.getValue();

                //Wifi?
                boolean setsWifi = setWifi.getBoolean(cle, false);
                boolean wifiOnOff;
                String wifiChoisi;
                String wifiIsOnOff = null;
                if (setsWifi){
                    wifiChoisi = "activé";
                    wifiOnOff = setValWifi.getBoolean(cle, false);
                    if(wifiOnOff)
                        wifiIsOnOff = "Allumé";
                    else
                        wifiIsOnOff = "Éteint";
                }
                else
                    wifiChoisi = "désactivé";

                //Mode Silencieux?
                boolean setsMute = setMute.getBoolean(cle, false);
                boolean muOnOff;
                String muteChoisi;
                String muteIsOnOff = null;
                if (setsMute) {
                    muteChoisi = "activé";
                    muOnOff = setValMute.getBoolean(cle, false);
                    if (muOnOff)
                        muteIsOnOff = "Silencieux";
                    else
                        muteIsOnOff = "Audible";
                }
                else
                    muteChoisi = "désactivé";

                //Choix Volume?
                int setsVol = setVolume.getInt(cle,0);
                String volChoisi;
                if(setsVol==0)
                    volChoisi = "désactivé";
                else{
                    volChoisi = "activé";
                }

                //Choix Bluetooth?
                boolean setsBt = setBluetooth.getBoolean(cle, false);
                boolean btOnOff;
                String btChoisi;
                String btIsOnOff = null;
                if (setsBt) {
                    btChoisi = "activé";
                    btOnOff = valBt.getBoolean(cle, false);
                    if (btOnOff)
                        btIsOnOff = "Allumé";
                    else
                        btIsOnOff = "Éteint";
                }
                else
                    btChoisi = "désactivé";

                //Lancement App?
                boolean launchApp = setApp.getBoolean(cle, false);
                String appChecked;
                String nomApp = null;
                if(launchApp){
                    appChecked = "activé";
                    nomApp = appChoisie.getString(cle, null);
                }
                else
                    appChecked  ="désactivé";

                //On écrit les différents paramètres dans la variable texte
                texte = texte + "ID = " + cle + " " +
                        "\n→Nom =" + val +
                        " \n→Contrôle wifi = " + wifiChoisi;
                        if (setsWifi)
                            texte = texte+";\n            Met à l'état: "+wifiIsOnOff;
                texte = texte + "\n→Contrôle Silencieux = "+muteChoisi;
                        if(setsMute)
                            texte = texte+";\n            Met à l'état: "+muteIsOnOff;
                texte = texte+"\n→Contrôle Volume = "+volChoisi;
                         if (setsVol>0){
                             texte = texte+";\n           Volume choisi = "+setsVol;
                         }
                texte = texte + "\n→Contrôle Bluetooth = "+ btChoisi;
                        if (setsBt){
                            texte = texte + ";\n            Met à l'état: " +btIsOnOff;
                        }
                texte = texte + "\n→Lancement Application = "+ appChecked;
                if (launchApp){
                    texte = texte + ";\n            Lance l'application: " +nomApp;
                }
                texte = texte+"\n \n";
            }
            tv.setText(texte);
            histVisible = !histVisible;
        }
    }

    public void effacerHist(View view){
        sharedPrefId = this.getSharedPreferences("preferenceCleId", Context.MODE_PRIVATE);
        effaceSP(sharedPrefId);

        setWifi = this.getSharedPreferences("preferenceIdWifi", Context.MODE_PRIVATE);
        effaceSP(setWifi);
        //setValWifi?;

        setMute = this.getSharedPreferences("preferenceIdMute", Context.MODE_PRIVATE);
        effaceSP(setMute);
        //setValMute?;

        setVolume = this.getSharedPreferences(getString(R.string.preferenceIdVol), Context.MODE_PRIVATE);
        effaceSP(setVolume);

        setBluetooth = this.getSharedPreferences(getString(R.string.preferenceIdBt), Context.MODE_PRIVATE);
        effaceSP(setBluetooth);

        valBt = this.getSharedPreferences(getString(R.string.preferenceIdValBt), Context.MODE_PRIVATE);
        effaceSP(valBt);

        setApp = this.getSharedPreferences("preferenceIdApp", Context.MODE_PRIVATE) ;
        appChoisie = this.getSharedPreferences("preferenceIdAppCh", MODE_PRIVATE) ;

        effaceSP(setApp);
        effaceSP(appChoisie);

        Toast.makeText(this, "Historique effacé", Toast.LENGTH_SHORT).show();
        monTexte.setText(R.string.defaultMessage);
    }

    //Efface les valeurs enregistrées dans l'objet SharedPreferences
    public void effaceSP(SharedPreferences SP){
        SharedPreferences.Editor editSP = SP.edit();
        editSP.clear();
        editSP.commit();
    }


    public void montrerValEnreg(View view){
        sharedPrefId = this.getSharedPreferences("preferenceCleId", Context.MODE_PRIVATE);
        allPreferences = sharedPrefId.getAll();
        showAllPreferences(monTexte, allPreferences);
    }

    //Actions effectuées quand parametrage validé
    public void validParam (View view){
        //Récupère le nom donné
        nomDonne = tagNamingBox.getText().toString();
        ajouteEntreeStringSP(detectedId, nomDonne, sharedPrefId);  /*Cle : ID et Valeur = nom*/
        tagNamingBox.setText("");
        tagNamingBox.setVisibility(GONE);

        //Récupère Params Wifi
        setWifi = this.getSharedPreferences("preferenceIdWifi", Context.MODE_PRIVATE);
        setValWifi = this.getSharedPreferences(getString(R.string.preferenceIdValWifi), Context.MODE_PRIVATE);
        activWifi = (CheckBox) findViewById(R.id.checkBoxWifi);
        switchWifi = (Switch) findViewById(R.id.switchWifi);
        boolean wifiChoisi = activWifi.isChecked();
        if (wifiChoisi)
            ajouteEntreeBoolSP(detectedId, switchWifi.isChecked(), setValWifi);
        ajouteEntreeBoolSP(detectedId, wifiChoisi, setWifi);
        activWifi.setChecked(false);
        activWifi.setVisibility(GONE);
        switchWifi.setChecked(false);
        switchWifi.setVisibility(GONE);

        //Récupère params mode silencieux
        setMute = this.getSharedPreferences("preferenceIdMute", Context.MODE_PRIVATE);
        setValMute = this.getSharedPreferences(getString(R.string.preferenceIdValMute), Context.MODE_PRIVATE);
        activMute = (CheckBox) findViewById(R.id.checkBoxMute);
        switchMute = (Switch) findViewById(R.id.switchMute);
        boolean muteChoisi = activMute.isChecked();
        if(muteChoisi)
            ajouteEntreeBoolSP(detectedId, switchMute.isChecked(), setValMute);
        ajouteEntreeBoolSP(detectedId, muteChoisi, setMute);
        activMute.setChecked(false);
        activMute.setVisibility(GONE);
        switchMute.setChecked(false);
        switchMute.setVisibility(GONE);

        //Récupère params volume
        setVolume = this.getSharedPreferences("preferenceIdVol", Context.MODE_PRIVATE);
        activVol = (CheckBox)findViewById(R.id.checkBoxVolume);
        volSpinner = (Spinner)findViewById(R.id.spinnerVolume);
        boolean volChoisi = activVol.isChecked();
        int selectedVol=0;
        if(volChoisi){
            selectedVol = (int) volSpinner.getSelectedItem();
        }
        ajouteEntreeIntSP(detectedId, selectedVol, setVolume);
        activVol.setChecked(false);
        activVol.setVisibility(GONE);
        volSpinner.setVisibility(GONE);

        //Récupérer params Bt
        setBluetooth = this.getSharedPreferences(getString(R.string.preferenceIdBt), Context.MODE_PRIVATE);
        valBt = this.getSharedPreferences(getString(R.string.preferenceIdValBt), Context.MODE_PRIVATE);
        activBluetooth = (CheckBox) findViewById(R.id.checkBoxBluetooth);
        switchBluetooth = (Switch) findViewById(R.id.switchBluetooth);
        boolean btChoisi = activBluetooth.isChecked();
        boolean btOnOff = false;
        if (btChoisi){
            btOnOff = switchBluetooth.isChecked();
            ajouteEntreeBoolSP(detectedId, btOnOff, valBt);
        }
        ajouteEntreeBoolSP(detectedId, btChoisi, setBluetooth);
        activBluetooth.setChecked(false);
        activBluetooth.setVisibility(GONE);
        switchBluetooth.setChecked(false);
        switchBluetooth.setVisibility(GONE);

        //Récupère params app
        activApp = (CheckBox)findViewById(R.id.checkBoxApp);
        setApp = this.getSharedPreferences("preferenceIdApp", Context.MODE_PRIVATE);
        ajouteEntreeBoolSP(detectedId, activApp.isChecked(), setApp);

        if (activApp.isChecked()){
            spinnerApp = (Spinner)findViewById(R.id.spinnerApp);
            appChoisie = this.getSharedPreferences("preferenceIdAppCh", Context.MODE_PRIVATE);
            String nomAppChoisie = spinnerApp.getSelectedItem().toString();
            ajouteEntreeStringSP(detectedId, nomAppChoisie, appChoisie);
            spinnerApp.setVisibility(GONE);
       }
        activApp.setVisibility(GONE);

        Button boutonValider = (Button)findViewById(R.id.buttonValider);
        boutonValider.setVisibility(GONE);
        Toast.makeText(this, "Paramètres enregistrés", Toast.LENGTH_SHORT).show();
    }

    //Afficher le formulaire de parametrage
    public void afficherParametrage(){
        monTexte = (TextView) findViewById(R.id.monTexte);
        activWifi = (CheckBox) findViewById(R.id.checkBoxWifi);
        activMute = (CheckBox) findViewById(R.id.checkBoxMute);
        boutonValider = (Button) findViewById(R.id.buttonValider);
        activVol = (CheckBox) findViewById(R.id.checkBoxVolume);
        activBluetooth = (CheckBox)findViewById(R.id.checkBoxBluetooth);
        activApp = (CheckBox)findViewById(R.id.checkBoxApp);

        tagNamingBox.setVisibility(View.VISIBLE);
        activWifi.setVisibility(View.VISIBLE);
        activMute.setVisibility(View.VISIBLE);
        boutonValider.setVisibility(View.VISIBLE);
        activVol.setVisibility(View.VISIBLE);
        activBluetooth.setVisibility(View.VISIBLE);
        activApp.setVisibility(View.VISIBLE);

        tagNamingBox.requestFocus();
        imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(this.tagNamingBox, InputMethodManager.SHOW_FORCED);
    }

    public void cacherParametrage(){
        tagNamingBox = (EditText) findViewById(R.id.tagNamingBox);
        activWifi = (CheckBox) findViewById(R.id.checkBoxWifi);
        activMute = (CheckBox) findViewById(R.id.checkBoxMute);
        boutonValider = (Button) findViewById(R.id.buttonValider);
        activVol = (CheckBox) findViewById(R.id.checkBoxVolume);
        volSpinner = (Spinner)findViewById(R.id.spinnerVolume);
        activBluetooth= (CheckBox) findViewById(R.id.checkBoxBluetooth);
        switchBluetooth = (Switch)findViewById(R.id.switchBluetooth);
        switchWifi = (Switch)findViewById(R.id.switchWifi);
        switchMute = (Switch)findViewById(R.id.switchMute);
        activApp = (CheckBox)findViewById(R.id.checkBoxApp);
        spinnerApp = (Spinner)findViewById(R.id.spinnerApp);

        tagNamingBox.setVisibility(GONE);
        activWifi.setVisibility(GONE);
        activMute.setVisibility(GONE);
        activVol.setVisibility(GONE);
        boutonValider.setVisibility(GONE);
        volSpinner.setVisibility(GONE);
        activBluetooth.setVisibility(GONE);
        switchBluetooth.setVisibility(GONE);
        switchWifi.setVisibility(GONE);
        switchMute.setVisibility(GONE);
        activApp.setVisibility(GONE);
        spinnerApp.setVisibility(GONE);
    }

    //Agît comme un interrupteur wifi, selon la valeur du boléen en paramètre
    public void interrupteurWifi(boolean valWifi){
        wifiMan = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        boolean isWifiOn = wifiMan.isWifiEnabled();
        if(!valWifi && isWifiOn) {
            Toast.makeText(this, "Désactivation du wifi...", Toast.LENGTH_LONG).show();
            wifiMan.setWifiEnabled(valWifi);
        }
        else if(valWifi && !isWifiOn){
            Toast.makeText(this, "Activation du wifi...", Toast.LENGTH_LONG).show();
            wifiMan.setWifiEnabled(valWifi);
        }
    }

    //Active/désactive le mode silencieux, selon le booléen donné en entrée
    public void interrupteurSon(boolean valMute){
        audioManager = (AudioManager) this.getSystemService(this.AUDIO_SERVICE);
        if(valMute)
            audioManager.adjustVolume(AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI);
        else
            audioManager.adjustVolume(AudioManager.ADJUST_UNMUTE, AudioManager.FLAG_SHOW_UI);
    }

    public void choisirVolume(int vol){
        audioManager = (AudioManager) this.getSystemService(this.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, vol, AudioManager.FLAG_VIBRATE);
        //utiliser SFX
        //audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_DELETE);
    }

    //Interrupteur données cellulaires => NE MARCHE PLUS DANS LA DERNIERE VERSION
    private void setMobileDataEnabled(Context context, boolean enabled) {
        final ConnectivityManager conman = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Class conmanClass = null;
        try {
            conmanClass = Class.forName(conman.getClass().getName());
            final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
            iConnectivityManagerField.setAccessible(true);
            final Object iConnectivityManager = iConnectivityManagerField.get(conman);
            final Class iConnectivityManagerClass = Class.forName(iConnectivityManager.getClass().getName());
            final Method setMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
            setMobileDataEnabledMethod.setAccessible(true);
            setMobileDataEnabledMethod.invoke(iConnectivityManager, enabled);
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public void afficherSpinner(View view){
        //On empêche mute et volume d'être controllés en même temps
        activMute = (CheckBox)findViewById(R.id.checkBoxMute);
        activMute.setChecked(false);
        switchMute = (Switch)findViewById(R.id.switchMute);
        switchMute.setVisibility(GONE);

        volSpinner = (Spinner)findViewById(R.id.spinnerVolume);

        if(volSpinner.getVisibility()==View.GONE) {
            volSpinner.setVisibility(View.VISIBLE);
            // Create an ArrayAdapter using the string array and a default spinner layout
            Integer[] items = new Integer[]{1,2,3,4,5,6,7};
            ArrayAdapter<Integer> adapter = new ArrayAdapter<Integer>(this, android.R.layout.simple_spinner_item, items);
             // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            // Apply the adapter to the spinner
            volSpinner.setAdapter(adapter);
        }
        else if(volSpinner.getVisibility()==View.VISIBLE)
            volSpinner.setVisibility(GONE);
    }

    public void uncheckVol(View view){
        //On empêche Volume et Mute d'être controllés en même temps
        activVol = (CheckBox)findViewById(R.id.checkBoxVolume);
        volSpinner = (Spinner)findViewById(R.id.spinnerVolume);
        activVol.setChecked(false);
        volSpinner.setVisibility(GONE);

        //Affiche le switch pour mute
        afficherSwitchMute();
    }

    public void afficherSwitch(View view){
        activBluetooth = (CheckBox) findViewById(R.id.checkBoxBluetooth);
        switchBluetooth = (Switch) findViewById(R.id.switchBluetooth);

        if (activBluetooth.isChecked())
            switchBluetooth.setVisibility(View.VISIBLE);
        else
            switchBluetooth.setVisibility(GONE);
    }

    public void allumeEteintBt(boolean onOff){
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (onOff && !btAdapter.isEnabled())
            btAdapter.enable();
        else if (!onOff && btAdapter.isEnabled())
            btAdapter.disable();
    }

    public void afficherSwitchWifi(View view){
        activWifi = (CheckBox)findViewById(R.id.checkBoxWifi);
        switchWifi = (Switch)findViewById(R.id.switchWifi);
        if (activWifi.isChecked())
            switchWifi.setVisibility(View.VISIBLE);
        else
            switchWifi.setVisibility(GONE);
    }

    public void afficherSwitchMute(){
        activMute = (CheckBox)findViewById(R.id.checkBoxMute);
        switchWifi = (Switch)findViewById(R.id.switchMute);
        if (activMute.isChecked())
            switchMute.setVisibility(View.VISIBLE);
        else
            switchMute.setVisibility(GONE);
    }

    public void launchTheMaps(View view){
        // Create an Intent from gmmIntentUri. Set the action to ACTION_VIEW
                Intent mapIntent = new Intent(Intent.ACTION_VIEW);
        // Make the Intent explicit by setting the Google Maps package
                mapIntent.setPackage("com.google.android.apps.maps");
        // Attempt to start an activity that can handle the Intent
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        }
        else
            Toast.makeText(this, "Aucune application de cartographie détectée.", Toast.LENGTH_LONG);
    }

    public void getAllAppsInSpinner(){
        //On récupère une liste d'informations sur chacune des apps instalées sur l'appareil
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> listApp = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        //Dans un tableau, on mettra la liste des noms des applications
        List<CharSequence> listNameApp = new ArrayList<CharSequence>();
        List<Drawable>listIconApp = new ArrayList<>();
        Iterator<ApplicationInfo> appIter = listApp.iterator();
        while(appIter.hasNext()){
            ApplicationInfo currentIter = appIter.next();
            Intent myIntent = getPackageManager().getLaunchIntentForPackage(currentIter.packageName);
            if (myIntent!=null)
                listNameApp.add(currentIter.loadLabel(pm));
        }

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, listNameApp);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerApp = (Spinner) findViewById(R.id.spinnerApp);
        spinnerApp.setAdapter(adapter);
    }

    public void lanceApp(CharSequence labelApp){
        String res = null;

        //On récupère le nom du package et on le stocke dans la variable res
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> listApp = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        Iterator<ApplicationInfo> iterListApp = listApp.iterator();
        while(iterListApp.hasNext()){
            ApplicationInfo thisApp = iterListApp.next();
            CharSequence nextLabel = thisApp.loadLabel(pm);
            if(nextLabel.equals(labelApp))
                res = thisApp.packageName;
        }

        //Lancement de l'application
        Intent myIntent = getPackageManager().getLaunchIntentForPackage(res);
        List activities = pm.queryIntentActivities(myIntent, PackageManager.MATCH_DEFAULT_ONLY);
        boolean isIntentSafe = activities.size() > 0;
        // Tentative de lancement d'une activité capable de répondre à notre intent
        if (myIntent.resolveActivity(getPackageManager()) != null || isIntentSafe) {
            //try {
                startActivity(myIntent);
           // }
            //catch(Error e){
              // Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
           // }
        }
        else
            Toast.makeText(this, "Application choisie non trouvée, package : "+res, Toast.LENGTH_LONG).show();
    }

    public void afficherSpinnerApp(View view){
        activApp = (CheckBox)findViewById(R.id.checkBoxApp);
        spinnerApp = (Spinner)findViewById(R.id.spinnerApp);
        if (activApp.isChecked() && spinnerApp.getVisibility() == GONE){
            spinnerApp.setVisibility(View.VISIBLE);
            getAllAppsInSpinner();
        }
        else if(!activApp.isChecked() && spinnerApp.getVisibility() == VISIBLE)
            spinnerApp.setVisibility(GONE);
    }

    public void testApp(View view){
        spinnerApp = (Spinner) findViewById(R.id.spinnerApp);
        CharSequence charseq = (CharSequence) spinnerApp.getSelectedItem();
        String sttr  =(String) charseq;
        CharSequence charseqPrime = (CharSequence) sttr;
        lanceApp(sttr);
    }

}
