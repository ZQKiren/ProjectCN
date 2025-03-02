package com.example.myapp.data;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class Product implements Parcelable {

    private String id;
    private String name;
    private String category;
    private String description;
    private double price;
    private int offerPercentage;
    private List<String> sizes;
    private List<String> colors;
    private List<String> images;
    private int quantity;

    public Product() {}

    protected Product(Parcel in) {
        id = in.readString();
        name = in.readString();
        category = in.readString();
        description = in.readString();
        price = in.readDouble();
        offerPercentage = in.readInt();
        sizes = in.createStringArrayList();
        colors = in.createStringArrayList();
        images = in.createStringArrayList();
        quantity = in.readInt();
    }

    public Product(String name, String category, String description, double price, int discount, List<String> sizes, List<String> selectedColors, List<String> images, int quantity) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.price = price;
        this.offerPercentage = discount;
        this.sizes = sizes != null ? sizes : new ArrayList<>();
        this.colors = selectedColors != null ? selectedColors : new ArrayList<>();
        this.images = images != null ? images : new ArrayList<>();
        this.quantity = quantity;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(category);
        dest.writeString(description);
        dest.writeDouble(price);
        dest.writeInt(offerPercentage);
        dest.writeStringList(sizes);
        dest.writeStringList(colors);
        dest.writeStringList(images);
        dest.writeInt(quantity);
    }

    public double getDiscountedPrice() {
        return this.price * (100 - this.offerPercentage) / 100.0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Product> CREATOR = new Creator<>() {
        @Override
        public Product createFromParcel(Parcel in) {
            return new Product(in);
        }

        @Override
        public Product[] newArray(int size) {
            return new Product[size];
        }
    };

    // Getters and setters for fields
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public double getPrice() { return price; }
    public int getOfferPercentage() { return offerPercentage; }
    public List<String> getSizes() { return sizes != null ? sizes : new ArrayList<>(); }
    public List<String> getColors() { return colors != null ? colors : new ArrayList<>(); }
    public List<String> getImages() { return images != null ? images : new ArrayList<>(); }
    public int getQuantity() { return quantity; }

    public void setName(String name) { this.name = name; }
    public void setCategory(String category) { this.category = category; }
    public void setDescription(String description) { this.description = description; }
    public void setPrice(double price) { this.price = price; }
    public void setOfferPercentage(int offerPercentage) { this.offerPercentage = offerPercentage; }
    public void setSizes(List<String> sizes) { this.sizes = sizes != null ? sizes : new ArrayList<>(); }
    public void setColors(List<String> colors) { this.colors = colors != null ? colors : new ArrayList<>(); }
    public void setImages(List<String> images) { this.images = images != null ? images : new ArrayList<>(); }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
