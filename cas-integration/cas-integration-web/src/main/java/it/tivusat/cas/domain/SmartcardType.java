package it.tivusat.cas.domain;

public enum SmartcardType {

    TIVU_4K("10", "Tivu4K"),
    TIVU_HD("11", "TivuHD"),
    TIVU_RAI("12", "TivuRAI"),
    TIVU_PRO("13", "TivuPRO"),
    TIVU_4K_SIM("14", "Tivu4KSIM"),
    TIVU_HD_SIM("16", "TivuHDSIM"),
    TIVU_HD_PAIRING("17", "TivuHDPairing"),
    TIVU_4K_ANDROID("18", "Tivu4KAndroid"),
    TIVU_4K_DIGIQUEST("19", "Tivu4KDigiquest"),
    TIVU_VIRTUAL(null, "TivuVirtual");

    private final String smsArticle;
    private final String nagraType;

    SmartcardType(String smsArticle, String nagraType) {
        this.smsArticle = smsArticle;
        this.nagraType = nagraType;
    }

    public String getSmsArticle() {
        return smsArticle;
    }

    public String getNagraType() {
        return nagraType;
    }
}