package com.boymask.edocs.passport;

import static org.jmrtd.PassportService.DEFAULT_MAX_BLOCKSIZE;
import static org.jmrtd.PassportService.NORMAL_MAX_TRANCEIVE_LENGTH;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.nfc.tech.IsoDep;
import android.os.AsyncTask;

import android.util.Log;

import com.boymask.edocs.CardData;
import com.gemalto.jp2.JP2Decoder;

import net.sf.scuba.smartcards.CardFileInputStream;
import net.sf.scuba.smartcards.CardService;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.x509.Certificate;
import org.jmrtd.BACKeySpec;
import org.jmrtd.PassportService;
import org.jmrtd.lds.CardAccessFile;
import org.jmrtd.lds.ChipAuthenticationPublicKeyInfo;
import org.jmrtd.lds.PACEInfo;
import org.jmrtd.lds.SODFile;
import org.jmrtd.lds.SecurityInfo;
import org.jmrtd.lds.icao.DG14File;
import org.jmrtd.lds.icao.DG1File;
import org.jmrtd.lds.icao.DG2File;
import org.jmrtd.lds.icao.MRZInfo;
import org.jmrtd.lds.iso19794.FaceImageInfo;
import org.jmrtd.lds.iso19794.FaceInfo;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.X509Certificate;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;


public class ReadTask extends AsyncTask<Void, Void, Exception> {
    private boolean encodePhotoToBase64 = false;
    private static final String TAG = "Reader";
    private final Activity parent;
    private IsoDep isoDep;
    private BACKeySpec bacKey;


    private DG1File dg1File;
    private DG2File dg2File;
    private DG14File dg14File;
    private SODFile sodFile;
    private String imageBase64;
    private Bitmap bitmap;
    private boolean chipAuthSucceeded = false;
    private boolean passiveAuthSuccess = false;
    private Bitmap photo;
    private byte[] dg14Encoded = new byte[0];


    ReadTask(Activity parent, IsoDep isoDep, BACKeySpec bacKey) {
        this.isoDep = isoDep;
        this.bacKey = bacKey;
        this.parent=parent;
    }

    private void doChipAuth(PassportService service) {
        try {
            CardFileInputStream dg14In = service.getInputStream(PassportService.EF_DG14);
            dg14Encoded = IOUtils.toByteArray(dg14In);
            ByteArrayInputStream dg14InByte = new ByteArrayInputStream(dg14Encoded);
            dg14File = new DG14File(dg14InByte);

            Collection<SecurityInfo> dg14FileSecurityInfos = dg14File.getSecurityInfos();
            for (SecurityInfo securityInfo : dg14FileSecurityInfos) {
                if (securityInfo instanceof ChipAuthenticationPublicKeyInfo) {
                    ChipAuthenticationPublicKeyInfo publicKeyInfo = (ChipAuthenticationPublicKeyInfo) securityInfo;
                    BigInteger keyId = publicKeyInfo.getKeyId();
                    PublicKey publicKey = publicKeyInfo.getSubjectPublicKey();
                    String oid = publicKeyInfo.getObjectIdentifier();
                    service.doEACCA(keyId, ChipAuthenticationPublicKeyInfo.ID_CA_ECDH_AES_CBC_CMAC_256, oid, publicKey);
                    chipAuthSucceeded = true;
                }
            }
        }
        catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    private void doPassiveAuth() {
        try {
            MessageDigest digest = MessageDigest.getInstance(sodFile.getDigestAlgorithm());

            Map<Integer,byte[]> dataHashes = sodFile.getDataGroupHashes();

            byte[] dg14Hash = new byte[0];
            if(chipAuthSucceeded) {
                dg14Hash = digest.digest(dg14Encoded);
            }
            byte[] dg1Hash = digest.digest(dg1File.getEncoded());
            byte[] dg2Hash = digest.digest(dg2File.getEncoded());

            if(Arrays.equals(dg1Hash, dataHashes.get(1)) && Arrays.equals(dg2Hash, dataHashes.get(2)) && (!chipAuthSucceeded || Arrays.equals(dg14Hash, dataHashes.get(14)))) {
                // We retrieve the CSCA from the german master list
                ASN1InputStream asn1InputStream = new ASN1InputStream(parent.getAssets().open("masterList"));
                ASN1Primitive p;
                KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
                keystore.load(null, null);
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                while((p = asn1InputStream.readObject()) != null) {
                    ASN1Sequence asn1 = ASN1Sequence.getInstance(p);
                    if (asn1 == null || asn1.size() == 0) {
                        throw new IllegalArgumentException("null or empty sequence passed.");
                    }
                    if (asn1.size() != 2) {
                        throw new IllegalArgumentException("Incorrect sequence size: " + asn1.size());
                    }
                    ASN1Set certSet = ASN1Set.getInstance(asn1.getObjectAt(1));

                    for (int i = 0; i < certSet.size(); i++) {
                        Certificate certificate = Certificate.getInstance(certSet.getObjectAt(i));

                        byte[] pemCertificate = certificate.getEncoded();

                        java.security.cert.Certificate javaCertificate = cf.generateCertificate(new ByteArrayInputStream(pemCertificate));
                        keystore.setCertificateEntry(String.valueOf(i), javaCertificate);
                    }
                }
                List<X509Certificate> docSigningCertificates = sodFile.getDocSigningCertificates();
                for (X509Certificate docSigningCertificate : docSigningCertificates) {
                    docSigningCertificate.checkValidity();
                }

                // We check if the certificate is signed by a trusted CSCA
                // TODO: verify if certificate is revoked
                CertPath cp = cf.generateCertPath(docSigningCertificates);
                PKIXParameters pkixParameters = new PKIXParameters(keystore);
                pkixParameters.setRevocationEnabled(false);
                CertPathValidator cpv = CertPathValidator.getInstance(CertPathValidator.getDefaultType());
                cpv.validate(cp, pkixParameters);

                String sodDigestEncryptionAlgorithm = sodFile.getDigestEncryptionAlgorithm();

                boolean isSSA = false;
                if (sodDigestEncryptionAlgorithm.equals("SSAwithRSA/PSS")) {
                    sodDigestEncryptionAlgorithm = "SHA256withRSA/PSS";
                    isSSA = true;
                }

                Signature sign = Signature.getInstance(sodDigestEncryptionAlgorithm);
                if (isSSA) {
                    sign.setParameter(new PSSParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 32, 1));
                }

                sign.initVerify(sodFile.getDocSigningCertificate());
                sign.update(sodFile.getEContent());
                passiveAuthSuccess = sign.verify(sodFile.getEncryptedDigest());
            }
        }
        catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    @Override
    protected Exception doInBackground(Void... params) {
        try {
            CardService cardService = CardService.getInstance(isoDep);
            cardService.open();

            PassportService service = new PassportService(cardService, NORMAL_MAX_TRANCEIVE_LENGTH, DEFAULT_MAX_BLOCKSIZE, false, false);
            service.open();

            boolean paceSucceeded = false;
            try {
                CardAccessFile cardAccessFile = new CardAccessFile(service.getInputStream(PassportService.EF_CARD_ACCESS));
                Collection<SecurityInfo> securityInfoCollection = cardAccessFile.getSecurityInfos();
                for (SecurityInfo securityInfo : securityInfoCollection) {
                    if (securityInfo instanceof PACEInfo) {
                        PACEInfo paceInfo = (PACEInfo) securityInfo;
                        service.doPACE(bacKey, paceInfo.getObjectIdentifier(), PACEInfo.toParameterSpec(paceInfo.getParameterId()), null);
                        paceSucceeded = true;
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, e);
            }

            service.sendSelectApplet(paceSucceeded);

            if (!paceSucceeded) {
                try {
                    service.getInputStream(PassportService.EF_COM).read();
                } catch (Exception e) {
                    service.doBAC(bacKey);
                }
            }

            CardFileInputStream dg1In = service.getInputStream(PassportService.EF_DG1);
            dg1File = new DG1File(dg1In);

            CardFileInputStream dg2In = service.getInputStream(PassportService.EF_DG2);
            dg2File = new DG2File(dg2In);

            CardFileInputStream sodIn = service.getInputStream(PassportService.EF_SOD);
            sodFile = new SODFile(sodIn);

            // We perform Chip Authentication using Data Group 14
            doChipAuth(service);

            // Then Passive Authentication using SODFile
            doPassiveAuth();

            List<FaceImageInfo> allFaceImageInfos = new ArrayList<>();
            List<FaceInfo> faceInfos = dg2File.getFaceInfos();
            for (FaceInfo faceInfo : faceInfos) {
                allFaceImageInfos.addAll(faceInfo.getFaceImageInfos());
            }

            if (!allFaceImageInfos.isEmpty()) {
                FaceImageInfo faceImageInfo = allFaceImageInfos.iterator().next();

                int imageLength = faceImageInfo.getImageLength();
                DataInputStream dataInputStream = new DataInputStream(faceImageInfo.getImageInputStream());
                byte[] buffer = new byte[imageLength];
                dataInputStream.readFully(buffer, 0, imageLength);
                InputStream inputStream = new ByteArrayInputStream(buffer, 0, imageLength);
         //      photo = BitmapFactory.decodeByteArray(buffer,0,imageLength);
                photo = new JP2Decoder(buffer).decode();

            //    bitmap = ImageUtil.decodeImage(
             //           parent, faceImageInfo.getMimeType(), inputStream);
              //  imageBase64 = Base64.encodeToString(buffer, Base64.DEFAULT);
            }

        } catch (Exception e) {
            return e;
        }
        return null;
    }

    @Override
    protected void onPostExecute(Exception result) {


        if (result == null) {

            Intent intent;
            if (parent.getCallingActivity() != null) {
                intent = new Intent();
            } else {
     //           intent = new Intent(parent, ResultActivity.class);
            }

            MRZInfo mrzInfo = dg1File.getMRZInfo();
/*
            intent.putExtra(ResultActivity.KEY_FIRST_NAME, mrzInfo.getSecondaryIdentifier().replace("<", " "));
            intent.putExtra(ResultActivity.KEY_LAST_NAME, mrzInfo.getPrimaryIdentifier().replace("<", " "));
            intent.putExtra(ResultActivity.KEY_GENDER, mrzInfo.getGender().toString());
            intent.putExtra(ResultActivity.KEY_STATE, mrzInfo.getIssuingState());
            intent.putExtra(ResultActivity.KEY_NATIONALITY, mrzInfo.getNationality());
*/
            CardData data = new CardData();
            data.setCognome(mrzInfo.getPrimaryIdentifier().replace("<", " "));
            data.setNome(mrzInfo.getSecondaryIdentifier().replace("<", " "));
            data.setSex(mrzInfo.getGender().toString());
            data.setState(mrzInfo.getIssuingState());
            data.setDataFineValidita(mrzInfo.getDateOfExpiry());
      //      data.setDataInizioValidita(mrzInfo.getIss);
            data.setNationality(mrzInfo.getNationality());
            data.setDataNascita(mrzInfo.getDateOfBirth());
            data.setDocNumber(mrzInfo.getDocumentNumber());
            String op1 = mrzInfo.getOptionalData1();
            String s2 =  mrzInfo.getOptionalData2();
            data.setPhoto(photo);
String s3 = mrzInfo.getPersonalNumber();

        ((PassportActivity)parent).showResult(data);

            String passiveAuthStr = "";
            if(passiveAuthSuccess) {
                passiveAuthStr = "OK";//parent.getString(R.string.pass);
            } else {
                passiveAuthStr = "Fail";//parent.getString(R.string.failed);
            }

            String chipAuthStr = "";
            if (chipAuthSucceeded) {
                chipAuthStr = "OK";//parent.getString(R.string.pass);
            } else {
                chipAuthStr = "Fail";//parent.getString(R.string.failed);
            }
            /*
            intent.putExtra(ResultActivity.KEY_PASSIVE_AUTH, passiveAuthStr);
            intent.putExtra(ResultActivity.KEY_CHIP_AUTH, chipAuthStr);

            if (bitmap != null) {
                if (encodePhotoToBase64) {
                    intent.putExtra(ResultActivity.KEY_PHOTO_BASE64, imageBase64);
                } else {
                    double ratio = 320.0 / bitmap.getHeight();
                    int targetHeight = (int) (bitmap.getHeight() * ratio);
                    int targetWidth = (int) (bitmap.getWidth() * ratio);

                    intent.putExtra(ResultActivity.KEY_PHOTO,
                            Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, false));
                }
            }
            */

if(1<2)return;
            if (parent.getCallingActivity() != null) {
                parent.setResult(Activity.RESULT_OK, intent);
                parent.finish();
            } else {
                parent.startActivity(intent);
            }

        } else {
         //   Snackbar.make(passportNumberView, exceptionStack(result), Snackbar.LENGTH_LONG).show();
        }
    }

}