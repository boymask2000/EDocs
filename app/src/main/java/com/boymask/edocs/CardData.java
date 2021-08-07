package com.boymask.edocs;

public class CardData {
    private String dataInizioValidita;
    private String dataFineValidita;
    private String cognome;
    private String nome;
    private String dataNascita;
    private String sex;
    private String codFiscale;

    public String getDataInizioValidita() {
        return dataInizioValidita;
    }

    public void setDataInizioValidita(String dataInizioValidita) {
        this.dataInizioValidita = dataInizioValidita;
    }

    public String getDataFineValidita() {
        return dataFineValidita;
    }

    public void setDataFineValidita(String dataFineValidita) {
        this.dataFineValidita = dataFineValidita;
    }

    public String getCognome() {
        return cognome;
    }

    public void setCognome(String cognome) {
        this.cognome = cognome;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDataNascita() {
        return dataNascita;
    }

    public void setDataNascita(String dataNascita) {
        this.dataNascita = dataNascita;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getCodFiscale() {
        return codFiscale;
    }

    public void setCodFiscale(String codFiscale) {
        this.codFiscale = codFiscale;
    }

    @Override
    public String toString() {
        return "CardData{" +
                "dataInizioValidita='" + dataInizioValidita + '\'' +
                ", dataFineValidita='" + dataFineValidita + '\'' +
                ", cognome='" + cognome + '\'' +
                ", nome='" + nome + '\'' +
                ", dataNascita='" + dataNascita + '\'' +
                ", sex='" + sex + '\'' +
                ", codFiscale='" + codFiscale + '\'' +
                '}';
    }
}
