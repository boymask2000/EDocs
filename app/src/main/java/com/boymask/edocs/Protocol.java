package com.boymask.edocs;

public class Protocol {
    public static String parseProtocolParameters(StringBuilder sb, byte[] uid,
                                                  short sak, byte[] atqa, byte[] ats) {
        boolean success = true;
        sb.append("UID: " + bin2hex(uid) + "\n\n");
        sb.append("SAK: 0x" + Integer.toHexString(sak & 0xFF) + "\n");
        if ((sak & 0x20) != 0) {
            sb.append("    (OK) ISO-DEP bit (0x20) is set.\n");
        } else {
            success = false;
            sb.append("    (FAIL) ISO-DEP bit (0x20) is NOT set.\n");
        }
        if ((sak & 0x40) != 0) {
            sb.append("    (OK) P2P bit (0x40) is set.\n");
        } else {
            sb.append("    (WARN) P2P bit (0x40) is NOT set.\n");
        }
        sb.append("\n");
        sb.append("ATQA: " + bin2hex(atqa) + "\n");
        sb.append("\n");
        sb.append("ATS: " + bin2hex(ats) + "\n");
        sb.append("    TL: 0x" + Integer.toHexString(ats[0] & 0xFF) + "\n");
        sb.append("    T0: 0x" + Integer.toHexString(ats[1] & 0xFF) + "\n");
        boolean ta_present = false;
        boolean tb_present = false;
        boolean tc_present = false;
        int atsIndex = 1;
        if ((ats[atsIndex] & 0x40) != 0) {
            sb.append("        (OK) T(C) is present (bit 7 is set).\n");
            tc_present = true;
        } else {
            success = false;
            sb.append("        (FAIL) T(C) is not present (bit 7 is NOT set).\n");
        }
        if ((ats[atsIndex] & 0x20) != 0) {
            sb.append("        (OK) T(B) is present (bit 6 is set).\n");
            tb_present = true;
        } else {
            success = false;
            sb.append("        (FAIL) T(B) is not present (bit 6 is NOT set).\n");
        }
        if ((ats[atsIndex] & 0x10) != 0) {
            sb.append("        (OK) T(A) is present (bit 5 is set).\n");
            ta_present = true;
        } else {
            success = false;
            sb.append("        (FAIL) T(A) is not present (bit 5 is NOT set).\n");
        }
        int fsc = ats[atsIndex] & 0x0F;
        if (fsc > 8) {
            success = false;
            sb.append("        (FAIL) FSC " + Integer.toString(fsc) + " is > 8\n");
        } else if (fsc < 2) {
            sb.append("        (FAIL EMVCO) FSC " + Integer.toString(fsc) + " is < 2\n");
        } else {
            sb.append("        (OK) FSC = " + Integer.toString(fsc) + "\n");
        }
        atsIndex++;
        if (ta_present) {
            sb.append("    TA: 0x" + Integer.toHexString(ats[atsIndex] & 0xff) + "\n");
            if ((ats[atsIndex] & 0x80) != 0) {
                sb.append("        (OK) bit 8 set, indicating only same bit rate divisor.\n");
            } else {
                sb.append("        (FAIL EMVCO) bit 8 NOT set, indicating support for assymetric " +
                        "bit rate divisors. EMVCo requires bit 8 set.\n");
            }
            if ((ats[atsIndex] & 0x70) != 0) {
                sb.append("        (FAIL EMVCO) EMVCo requires bits 7 to 5 set to 0.\n");
            } else {
                sb.append("        (OK) bits 7 to 5 indicating only 106 kbit/s L->P supported.\n");
            }
            if ((ats[atsIndex] & 0x7) != 0) {
                sb.append("        (FAIL EMVCO) EMVCo requires bits 3 to 1 set to 0.\n");
            } else {
                sb.append("        (OK) bits 3 to 1 indicating only 106 kbit/s P->L supported.\n");
            }
            atsIndex++;
        }
        if (tb_present) {
            sb.append("    TB: 0x" + Integer.toHexString(ats[3] & 0xFF) + "\n");
            int fwi = (ats[atsIndex] & 0xF0) >> 4;
            if (fwi > 8) {
                success = false;
                sb.append("        (FAIL) FWI=" + Integer.toString(fwi) + ", should be <= 8\n");
            } else if (fwi == 8) {
                sb.append("        (FAIL EMVCO) FWI=" + Integer.toString(fwi) +
                        ", EMVCo requires <= 7\n");
            } else {
                sb.append("        (OK) FWI=" + Integer.toString(fwi) + "\n");
            }
            int sfgi = ats[atsIndex] & 0x0F;
            if (sfgi > 8) {
                success = false;
                sb.append("        (FAIL) SFGI=" + Integer.toString(sfgi) + ", should be <= 8\n");
            } else {
                sb.append("        (OK) SFGI=" + Integer.toString(sfgi) + "\n");
            }
            atsIndex++;
        }
        return sb.toString();
    }

    public static String bin2hex(byte[] bin) {
        String hexdigits = "0123456789ABCDEF";
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < bin.length; i++) {
            int value = bin[i] < 0 ? bin[i] + 256 : bin[i]; // signed -> unsigned
            buf.append(hexdigits.charAt(value / 16));
            buf.append(hexdigits.charAt(value % 16));
        }
        return buf.toString();
    }
}
