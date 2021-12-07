package com.amolg.flutterbarcodescanner;

import java.util.HashMap;
import com.google.android.gms.vision.barcode.Barcode;

public class BarcodeTypes {
    HashMap<Integer, String> typesMap = new HashMap<Integer, String>();

    BarcodeTypes(){
        typesMap.put(Barcode.AZTEC,"AZTEC");
        typesMap.put(Barcode.CODE_128,"CODE_128");
        typesMap.put(Barcode.CODE_39,"CODE_39");
        typesMap.put(Barcode.CODE_93,"CODE_93");
        typesMap.put(Barcode.CODABAR,"CODABAR");
        typesMap.put(Barcode.DATA_MATRIX,"DATA_MATRIX");
        typesMap.put(Barcode.EAN_13,"EAN_13");
        typesMap.put(Barcode.EAN_8,"EAN_8");
        typesMap.put(Barcode.ITF,"ITF");
        typesMap.put(Barcode.QR_CODE,"QR_CODE");
        typesMap.put(Barcode.UPC_A,"UPC_A");
        typesMap.put(Barcode.UPC_E,"UPC_E");
        typesMap.put(Barcode.PDF417,"PDF417");
    }

    public String getFormat(int format) {
        if(typesMap.containsKey(format)) {
            return typesMap.get(format);
        }else{
            return new String("UNKNOWN");
        }
    }
}