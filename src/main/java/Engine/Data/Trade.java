package Engine.Data;

import java.util.Date;

/**
 * Created by luke on 6/4/16.
 */
public class Trade {
    private String ric;
    private int qouteId;
    private int venueId;
    private double price;
    private Date tradeTime;
    private double tradeVolume;
    private byte tradeClassification;
    private String qualifiers;

    public Trade(String ric, int qouteId, int venueId, double price, Date tradeTime, double tradeVolume, byte tradeClassification, String qualifiers) {
        this.ric = ric;
        this.qouteId = qouteId;
        this.venueId = venueId;
        this.price = price;
        this.tradeTime = tradeTime;
        this.tradeVolume = tradeVolume;
        this.tradeClassification = tradeClassification;
        this.qualifiers = qualifiers;
    }

    public String getRic() {
        return ric;
    }

    public int getQouteId() {
        return qouteId;
    }

    public int getVenueId() {
        return venueId;
    }

    public double getPrice() {
        return price;
    }


    public Date getTradeTime() {
        return tradeTime;
    }

    public double getTradeVolume() {
        return tradeVolume;
    }

    public byte getTradeClassification() {
        return tradeClassification;
    }

    public String getQualifiers() {
        return qualifiers;
    }
}
