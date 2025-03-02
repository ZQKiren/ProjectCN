package com.example.myapp.fragments;

import android.app.Activity;
import android.content.Intent;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapp.R;
import com.example.myapp.activities.MainActivity;
import com.example.myapp.adapters.HomeAdapter;
import com.example.myapp.data.Product;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerViewProducts;
    private HomeAdapter productAdapter;
    private final List<Product> productList = new ArrayList<>();
    private SearchView searchView;

    private Spinner categorySpinner;
    private Spinner sortSpinner;
    private List<String> categories;
    private final List<Product> originalProductList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerViewProducts = view.findViewById(R.id.recycler_view_home);
        recyclerViewProducts.setLayoutManager(new GridLayoutManager(getContext(), 2));

        productAdapter = new HomeAdapter(productList);
        recyclerViewProducts.setAdapter(productAdapter);

        searchView = view.findViewById(R.id.search_view);

        setupSpinners(view);

        int searchEditId = searchView.getContext().getResources()
                .getIdentifier("android:id/search_src_text", null, null);
        EditText searchEditText = searchView.findViewById(searchEditId);

        if (searchEditText != null) {

            searchEditText.setTextColor(Color.BLACK);
            searchEditText.setHintTextColor(Color.GRAY);
            searchEditText.setBackgroundResource(android.R.color.transparent);
            searchEditText.setPadding(0, 0, 0, 0);

            // Xóa underline
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                searchEditText.setTextCursorDrawable(null);
            }
        }

        View searchPlate = searchView.findViewById(androidx.appcompat.R.id.search_plate);
        if (searchPlate != null) {
            searchPlate.setBackgroundColor(Color.TRANSPARENT);
        }

        ImageView micIcon = view.findViewById(R.id.mic_icon);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (query != null && !query.trim().isEmpty()) {
                    searchProducts(query);
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        micIcon.setOnClickListener(v -> startVoiceInput());
        loadProductsFromFirestore();
        return view;
    }

    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Nói để tìm kiếm...");

        if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
            speechRecognizerLauncher.launch(intent);
        } else {
            Toast.makeText(requireContext(), "Thiết bị không hỗ trợ nhận dạng giọng nói", Toast.LENGTH_SHORT).show();
        }
    }

    private final ActivityResultLauncher<Intent> speechRecognizerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    ArrayList<String> speechResults = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (speechResults != null && !speechResults.isEmpty()) {
                        String recognizedText = speechResults.get(0);
                        if (searchView != null) {
                            searchView.setIconified(false);
                            searchView.setQuery(recognizedText, false);
                            searchView.requestFocus();
                        }
                    } else {
                        Toast.makeText(requireContext(), "Không nhận được văn bản từ giọng nói", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(), "Lỗi khi nhận giọng nói", Toast.LENGTH_SHORT).show();
                }
            });

    private void setupSpinners(View view) {
        categorySpinner = view.findViewById(R.id.category_spinner);
        sortSpinner = view.findViewById(R.id.sort_spinner);

        categories = new ArrayList<>();
        categories.add("Tất cả");
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                categories
        );
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        // Thiết lập spinner sắp xếp
        String[] sortOptions = {"Mặc định", "Tên A-Z", "Tên Z-A", "Giá thấp-cao", "Giá cao-thấp"};
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                sortOptions
        );
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(sortAdapter);

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterProducts();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                sortProducts(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void loadProductsFromFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("products")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        productList.clear();
                        originalProductList.clear();
                        Set<String> categorySet = new HashSet<>();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Product product = document.toObject(Product.class);
                            product.setId(document.getId());
                            productList.add(product);
                            originalProductList.add(product);

                            // Thêm danh mục vào set
                            if (product.getCategory() != null && !product.getCategory().isEmpty()) {
                                categorySet.add(product.getCategory());
                            }
                        }

                        // Cập nhật danh sách danh mục
                        categories.clear();
                        categories.add("Tất cả");
                        categories.addAll(categorySet);
                        ((ArrayAdapter<?>)categorySpinner.getAdapter()).notifyDataSetChanged();

                        productAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(getContext(), "Lỗi khi tải sản phẩm", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void filterProducts() {
        String selectedCategory = categorySpinner.getSelectedItem().toString();

        if (selectedCategory.equals("Tất cả")) {
            productList.clear();
            productList.addAll(originalProductList);
        } else {
            productList.clear();
            for (Product product : originalProductList) {
                if (product.getCategory() != null && product.getCategory().equals(selectedCategory)) {
                    productList.add(product);
                }
            }
        }

        sortProducts(sortSpinner.getSelectedItemPosition());
    }

    private void sortProducts(int sortOption) {
        switch (sortOption) {
            case 1:
                productList.sort((p1, p2) ->
                        p1.getName().compareToIgnoreCase(p2.getName()));
                break;
            case 2:
                productList.sort((p1, p2) ->
                        p2.getName().compareToIgnoreCase(p1.getName()));
                break;
            case 3: // Giá thấp-cao (tính cả khuyến mãi)
                productList.sort(Comparator.comparingDouble(Product::getDiscountedPrice));
                break;
            case 4: // Giá cao-thấp (tính cả khuyến mãi)
                productList.sort((p1, p2) ->
                        Double.compare(p2.getDiscountedPrice(), p1.getDiscountedPrice()));
                break;
            default: // Mặc định - không sắp xếp
                if (categorySpinner.getSelectedItem().toString().equals("Tất cả")) {
                    productList.clear();
                    productList.addAll(originalProductList);
                }
                break;
        }
        productAdapter.notifyDataSetChanged();
    }

    private void searchProducts(String query) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("products")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Product> filteredList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Product product = document.toObject(Product.class);
                            if (product.getName().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))) {
                                filteredList.add(product);
                            }
                        }

                        // Chuyển đến SearchFragment với danh sách sản phẩm đã lọc
                        navigateToSearchFragment(filteredList, query);
                    } else {
                        Toast.makeText(getContext(), "Lỗi khi tìm kiếm sản phẩm", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToSearchFragment(List<Product> products, String query) {
        // Tạo SearchFragment và truyền dữ liệu
        SearchFragment searchFragment = new SearchFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList("filtered_products", new ArrayList<>(products));
        args.putString("search_query", query);
        searchFragment.setArguments(args);

        // Đặt trạng thái của BottomNavigationView
        if (getActivity() instanceof MainActivity mainActivity) {
            mainActivity.setBottomNavigationViewSelected(R.id.nav_search); // Đặt đúng ID của mục tìm kiếm
        }

        // Chuyển đến SearchFragment
        requireActivity().getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out) // Hiệu ứng chuyển đổi
                .replace(R.id.content_frame, searchFragment)
                .addToBackStack(null)
                .commit();
    }
}
