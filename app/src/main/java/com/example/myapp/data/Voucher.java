package com.example.myapp.data;

import android.os.Parcel;
import android.os.Parcelable;

public class Voucher implements Parcelable {
    private String id;
    private String code;
    private String description;
    private double discountAmount;
    private double minSpend;
    private long expiryDate;
    private boolean isUsed;
    private String userId;
    private int pointsRequired;// Số điểm cần để đổi voucher

    protected Voucher(Parcel in) {
        id = in.readString();
        code = in.readString();
        description = in.readString();
        discountAmount = in.readDouble();
        minSpend = in.readDouble();
        expiryDate = in.readLong();
        isUsed = in.readByte() != 0;
        userId = in.readString();
        pointsRequired = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(code);
        dest.writeString(description);
        dest.writeDouble(discountAmount);
        dest.writeDouble(minSpend);
        dest.writeLong(expiryDate);
        dest.writeByte((byte) (isUsed ? 1 : 0));
        dest.writeString(userId);
        dest.writeInt(pointsRequired);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Voucher> CREATOR = new Creator<>() {
        @Override
        public Voucher createFromParcel(Parcel in) {
            return new Voucher(in);
        }

        @Override
        public Voucher[] newArray(int size) {
            return new Voucher[size];
        }
    };

    public Voucher() {}

    public Voucher(String code, String description, double discountAmount,
                   double minSpend, long expiryDate, int pointsRequired) {
        this.code = code;
        this.description = description;
        this.discountAmount = discountAmount;
        this.minSpend = minSpend;
        this.expiryDate = expiryDate;
        this.pointsRequired = pointsRequired;
        this.isUsed = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(double discountAmount) { this.discountAmount = discountAmount; }
    public double getMinSpend() { return minSpend; }
    public void setMinSpend(double minSpend) { this.minSpend = minSpend; }
    public long getExpiryDate() { return expiryDate; }
    public void setExpiryDate(long expiryDate) { this.expiryDate = expiryDate; }
    public boolean isUsed() { return isUsed; }
    public void setUsed(boolean used) { isUsed = used; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public int getPointsRequired() { return pointsRequired; }
    public void setPointsRequired(int pointsRequired) { this.pointsRequired = pointsRequired; }

    // Các getter và setter khác giữ nguyên
}
