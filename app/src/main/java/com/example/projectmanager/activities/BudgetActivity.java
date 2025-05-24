package com.example.projectmanager.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.text.InputType;
import android.text.method.ScrollingMovementMethod;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projectmanager.R;
import com.example.projectmanager.adapters.BudgetAdapter;
import com.example.projectmanager.adapters.CategorySpinnerAdapter;
import com.example.projectmanager.models.Budget;
import com.example.projectmanager.utils.BudgetSorter;
import com.example.projectmanager.viewmodels.BudgetViewModel;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Hoạt động quản lý ngân sách được cải thiện với biểu đồ tối ưu và pagination
 */
public class BudgetActivity extends AppCompatActivity {
    private static final String TAG = "BudgetActivity";

    // Components giao diện
    private RecyclerView rvBudgets;
    private TextInputEditText etBudgetTitle, etBudgetAmount, etBudgetDescription;
    private MaterialAutoCompleteTextView spinnerCategory;
    private MaterialButton btnAddBudget, btnRefresh, btnLoadMore;
    private TextView tvTotalBudget, tvApprovedBudget;
    private ProgressBar progressBar;

    // Biểu đồ
    private PieChart budgetPieChart;
    private BarChart budgetStatusChart;

    // Adapter và dữ liệu
    private BudgetAdapter budgetAdapter;
    private List<Budget> budgetList;

    // ViewModel
    private BudgetViewModel budgetViewModel;

    // Formatter cho số tiền
    private DecimalFormat decimalFormat;

    // ID của budget đang được chỉnh sửa (nếu có)
    private String editingBudgetId = null;

    // Pagination controls
    private boolean isPaginationEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget);

        Log.d(TAG, "BudgetActivity được khởi tạo");

        // Khởi tạo ViewModel
        budgetViewModel = new ViewModelProvider(this).get(BudgetViewModel.class);

        // Khởi tạo formatter
        decimalFormat = new DecimalFormat("#,###,### VNĐ");

        // Thiết lập toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Quản lý ngân sách");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Khởi tạo giao diện
        if (!initViews()) {
            Log.e(TAG, "Không thể khởi tạo views");
            Toast.makeText(this, "Lỗi khởi tạo giao diện", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupRecyclerView();
        setupSpinner();
        setupSortDropdown();
        setupClickListeners();
        observeViewModel();

        // Enable pagination for better performance
        enablePagination();
    }

    /**
     * Khởi tạo các thành phần giao diện
     */
    private boolean initViews() {
        try {
            // Tìm RecyclerView
            rvBudgets = findViewById(R.id.rv_budgets);

            // Tìm form inputs
            etBudgetTitle = findViewById(R.id.et_budget_title);
            etBudgetAmount = findViewById(R.id.et_budget_amount);
            etBudgetDescription = findViewById(R.id.et_budget_description);
            spinnerCategory = findViewById(R.id.spinner_category);

            // Tìm buttons
            btnAddBudget = findViewById(R.id.btn_add_budget);
            btnRefresh = findViewById(R.id.btn_refresh);
            btnLoadMore = findViewById(R.id.btn_load_more); // Nút "Xem thêm"

            // Tìm summary TextViews
            tvTotalBudget = findViewById(R.id.tv_total_budget);
            tvApprovedBudget = findViewById(R.id.tv_approved_budget);

            // Tìm ProgressBar
            progressBar = findViewById(R.id.progress_bar);

            // Tìm Charts
            budgetPieChart = findViewById(R.id.budget_pie_chart);
            budgetStatusChart = findViewById(R.id.budget_status_chart);

            // Kiểm tra null cho các view quan trọng
            if (rvBudgets == null) {
                Log.e(TAG, "RecyclerView bị null");
                return false;
            }

            if (etBudgetTitle == null) {
                Log.e(TAG, "etBudgetTitle bị null");
                return false;
            }

            // Thiết lập focus và input type cho ô description
            if (etBudgetDescription != null) {
                etBudgetDescription.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                etBudgetDescription.setMinLines(2);
                etBudgetDescription.setMaxLines(4);
                etBudgetDescription.setVerticalScrollBarEnabled(true);
                etBudgetDescription.setMovementMethod(ScrollingMovementMethod.getInstance());
            }

            Log.d(TAG, "Khởi tạo giao diện thành công");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi khởi tạo views: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Thiết lập biểu đồ tròn cho ngân sách - Cải tiến
     * - Loại bỏ animation
     * - Cải thiện hiệu suất
     * - Tăng rõ ràng với font lớn hơn
     */
    private void setupBudgetPieChart() {
        if (budgetPieChart == null) {
            Log.e(TAG, "Biểu đồ tròn không tìm thấy");
            return;
        }

        try {
            // Cải thiện giao diện biểu đồ
            budgetPieChart.setUsePercentValues(true);
            budgetPieChart.getDescription().setEnabled(false);
            budgetPieChart.setExtraOffsets(15, 15, 15, 15); // Tăng padding

            // Loại bỏ hiệu ứng chuyển động để tăng hiệu suất
            budgetPieChart.setDragDecelerationEnabled(false);

            // Tùy chỉnh hole (lỗ trống ở giữa)
            budgetPieChart.setDrawHoleEnabled(true);
            budgetPieChart.setHoleColor(Color.WHITE);
            budgetPieChart.setHoleRadius(40f); // Tăng kích thước lỗ giữa
            budgetPieChart.setTransparentCircleRadius(45f); // Tăng vòng tròn trong suốt
            budgetPieChart.setTransparentCircleColor(Color.WHITE); // Màu vòng trong suốt

            // Tắt một số tính năng không cần thiết để tăng hiệu suất
            budgetPieChart.setDrawEntryLabels(false); // Tắt nhãn trên các phân đoạn
            budgetPieChart.setHighlightPerTapEnabled(false); // Tắt highlight khi nhấn

            // Tạo dữ liệu
            ArrayList<PieEntry> entries = new ArrayList<>();

            // Tính tổng cho mỗi danh mục
            float humanResourceAmount = getTotalForCategory("human_resource");
            float equipmentAmount = getTotalForCategory("equipment");
            float materialAmount = getTotalForCategory("material");
            float otherAmount = getTotalForCategory("other");

            // Chỉ thêm các mục có giá trị khác 0
            if (humanResourceAmount > 0) entries.add(new PieEntry(humanResourceAmount, "Nhân sự"));
            if (equipmentAmount > 0) entries.add(new PieEntry(equipmentAmount, "Thiết bị"));
            if (materialAmount > 0) entries.add(new PieEntry(materialAmount, "Vật liệu"));
            if (otherAmount > 0) entries.add(new PieEntry(otherAmount, "Khác"));

            // Kiểm tra xem có dữ liệu không
            if (entries.isEmpty()) {
                entries.add(new PieEntry(1, "Chưa có dữ liệu"));
            }

            // Tạo dataset và tùy chỉnh màu sắc
            PieDataSet dataSet = new PieDataSet(entries, "");
            dataSet.setSliceSpace(5f); // Tăng khoảng cách giữa các phân đoạn
            dataSet.setSelectionShift(5f); // Giảm mức độ "bật" khi chọn để tăng hiệu suất

            // Sử dụng màu sắc sáng và tương phản cao hơn
            ArrayList<Integer> colors = new ArrayList<>();
            colors.add(Color.parseColor("#2196F3")); // Xanh cho nhân sự
            colors.add(Color.parseColor("#00C853")); // Xanh lá cho thiết bị
            colors.add(Color.parseColor("#FFC107")); // Vàng cho vật liệu
            colors.add(Color.parseColor("#9C27B0")); // Tím cho mục khác
            dataSet.setColors(colors);

            // Tạo và định dạng đối tượng dữ liệu
            PieData pieData = new PieData(dataSet);
            pieData.setValueTextSize(14f); // Kích thước chữ vừa phải
            pieData.setValueTextColor(Color.WHITE); // Màu chữ trắng để dễ đọc
            pieData.setValueFormatter(new PercentFormatter(budgetPieChart));

            // Đặt dữ liệu và cập nhật
            budgetPieChart.setData(pieData);

            // Tùy chỉnh chú thích (legend)
            Legend legend = budgetPieChart.getLegend();
            legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
            legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
            legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
            legend.setDrawInside(false);
            legend.setTextSize(12f); // Kích thước chữ vừa phải cho chú thích
            legend.setFormSize(12f); // Kích thước ô màu
            legend.setFormToTextSpace(5f); // Khoảng cách giữa ô màu và văn bản
            legend.setXEntrySpace(10f); // Khoảng cách giữa các mục
            legend.setYEntrySpace(5f);

            // Thêm nhãn giữa biểu đồ
            budgetPieChart.setCenterText("Phân bổ\nngân sách");
            budgetPieChart.setCenterTextSize(14f);
            budgetPieChart.setCenterTextColor(Color.DKGRAY);

            // Cập nhật biểu đồ mà không dùng animation
            budgetPieChart.invalidate();

            Log.d(TAG, "Biểu đồ tròn đã được thiết lập và tối ưu");
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi thiết lập biểu đồ tròn: " + e.getMessage(), e);
        }
    }

    /**
     * Thiết lập biểu đồ cột cho tình trạng ngân sách - Cải tiến
     * - Loại bỏ animation
     * - Giảm kích thước dữ liệu
     * - Tối ưu hiệu suất
     */
    private void setupBudgetStatusBarChart() {
        if (budgetStatusChart == null) {
            Log.e(TAG, "Biểu đồ cột không tìm thấy");
            return;
        }

        try {
            // Cải thiện giao diện biểu đồ và tắt các tính năng không cần thiết
            budgetStatusChart.getDescription().setEnabled(false);
            budgetStatusChart.setDrawValueAboveBar(true);
            budgetStatusChart.setDrawGridBackground(false);
            budgetStatusChart.setDrawBarShadow(false);
            budgetStatusChart.setMaxVisibleValueCount(60);
            budgetStatusChart.setPinchZoom(false);
            budgetStatusChart.setDoubleTapToZoomEnabled(false); // Tắt zoom để tăng hiệu suất
            budgetStatusChart.setHighlightPerTapEnabled(false); // Tắt highlight
            budgetStatusChart.setExtraBottomOffset(10f); // Thêm khoảng cách ở dưới
            budgetStatusChart.setExtraTopOffset(10f); // Thêm khoảng cách ở trên

            // Cấu hình trục X
            XAxis xAxis = budgetStatusChart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setDrawGridLines(false);
            xAxis.setGranularity(1f);
            xAxis.setLabelCount(4);
            xAxis.setTextSize(10f); // Giảm kích thước chữ để rõ ràng hơn
            xAxis.setValueFormatter(new IndexAxisValueFormatter(new String[]{"Nhân sự", "Thiết bị", "Vật liệu", "Khác"}));

            // Cấu hình trục Y
            YAxis leftAxis = budgetStatusChart.getAxisLeft();
            leftAxis.setLabelCount(6, false);
            leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
            leftAxis.setSpaceTop(15f); // Giảm khoảng cách
            leftAxis.setAxisMinimum(0f);
            leftAxis.setDrawGridLines(true);
            leftAxis.setGridLineWidth(0.5f); // Làm mỏng đường lưới
            leftAxis.setGridColor(Color.LTGRAY); // Đổi màu đường lưới
            leftAxis.setTextSize(10f); // Kích thước chữ nhỏ hơn
            leftAxis.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    // Định dạng giá trị trục Y theo nghìn VNĐ
                    if (value >= 1000000) {
                        return Math.round(value / 1000000) + "M";
                    } else if (value >= 1000) {
                        return Math.round(value / 1000) + "K";
                    }
                    return String.valueOf((int)value);
                }
            });

            // Tắt trục Y bên phải
            budgetStatusChart.getAxisRight().setEnabled(false);

            // Tính toán dữ liệu cho ngân sách đã duyệt và chưa duyệt theo danh mục
            float[] approvedValues = new float[4]; // Cho 4 danh mục
            float[] pendingValues = new float[4];

            for (Budget budget : budgetList) {
                int categoryIndex;
                switch (budget.getCategory()) {
                    case "human_resource": categoryIndex = 0; break;
                    case "equipment": categoryIndex = 1; break;
                    case "material": categoryIndex = 2; break;
                    default: categoryIndex = 3; break; // "other"
                }

                if (budget.isApproved()) {
                    approvedValues[categoryIndex] += budget.getAmount();
                } else {
                    pendingValues[categoryIndex] += budget.getAmount();
                }
            }

            // Kiểm tra xem có dữ liệu không
            boolean hasData = false;
            for (int i = 0; i < 4; i++) {
                if (approvedValues[i] > 0 || pendingValues[i] > 0) {
                    hasData = true;
                    break;
                }
            }

            if (!hasData) {
                // Thêm dữ liệu mẫu nếu không có
                for (int i = 0; i < 4; i++) {
                    pendingValues[i] = 1;
                }
            }

            // Tạo entries cho mỗi danh mục
            ArrayList<BarEntry> approvedEntries = new ArrayList<>();
            ArrayList<BarEntry> pendingEntries = new ArrayList<>();

            for (int i = 0; i < 4; i++) {
                approvedEntries.add(new BarEntry(i, approvedValues[i]));
                pendingEntries.add(new BarEntry(i, pendingValues[i]));
            }

            // Màu sắc
            BarDataSet approvedSet = new BarDataSet(approvedEntries, "Đã duyệt");
            approvedSet.setColor(Color.parseColor("#4CAF50")); // Màu xanh lá
            approvedSet.setValueTextSize(10f); // Kích thước chữ nhỏ
            approvedSet.setValueTextColor(Color.BLACK);

            BarDataSet pendingSet = new BarDataSet(pendingEntries, "Chờ duyệt");
            pendingSet.setColor(Color.parseColor("#F44336")); // Màu đỏ
            pendingSet.setValueTextSize(10f); // Kích thước chữ nhỏ
            pendingSet.setValueTextColor(Color.BLACK);

            // Định dạng giá trị hiển thị và ẩn giá trị bằng 0
            ValueFormatter valueFormatter = new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    if (value == 0 || value == 1) return ""; // Ẩn giá trị 0 hoặc 1 (dữ liệu mẫu)
                    if (value >= 1000000) {
                        return Math.round(value / 1000000) + "M";
                    } else if (value >= 1000) {
                        return Math.round(value / 1000) + "K";
                    }
                    return String.valueOf((int)value);
                }
            };
            approvedSet.setValueFormatter(valueFormatter);
            pendingSet.setValueFormatter(valueFormatter);

            // Tạo dữ liệu thanh với cả hai datasets
            float groupSpace = 0.08f;
            float barSpace = 0.03f;
            float barWidth = 0.42f;

            BarData barData = new BarData(approvedSet, pendingSet);
            barData.setBarWidth(barWidth);

            // Nhóm các thanh và đặt dữ liệu
            budgetStatusChart.setData(barData);
            budgetStatusChart.groupBars(0, groupSpace, barSpace);

            // Cải thiện Legend
            Legend legend = budgetStatusChart.getLegend();
            legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
            legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
            legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
            legend.setDrawInside(false);
            legend.setFormSize(10f);
            legend.setTextSize(10f);
            legend.setXEntrySpace(10f);

            // Làm mới biểu đồ không dùng animation
            budgetStatusChart.invalidate();

            Log.d(TAG, "Biểu đồ cột đã được thiết lập và tối ưu hóa");
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi thiết lập biểu đồ cột: " + e.getMessage(), e);
        }
    }

    /**
     * Tính tổng cho mỗi danh mục
     */
    private float getTotalForCategory(String category) {
        float total = 0f;
        for (Budget budget : budgetList) {
            if (budget.getCategory().equals(category)) {
                total += (float) budget.getAmount();
            }
        }
        return total;
    }

    /**
     * Thiết lập RecyclerView
     */
    private void setupRecyclerView() {
        try {
            budgetList = new ArrayList<>();

            // Khởi tạo adapter với click listener
            budgetAdapter = new BudgetAdapter(budgetList, new BudgetAdapter.OnBudgetClickListener() {
                @Override
                public void onBudgetClick(Budget budget, int position) {
                    handleBudgetClick(budget);
                }

                @Override
                public void onBudgetLongClick(Budget budget, int position) {
                    handleBudgetLongClick(budget, position);
                }
            });

            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            rvBudgets.setLayoutManager(layoutManager);
            rvBudgets.setAdapter(budgetAdapter);

            Log.d(TAG, "RecyclerView được thiết lập");
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi thiết lập RecyclerView: " + e.getMessage(), e);
        }
    }

    /**
     * Thiết lập Spinner cho danh mục
     */
    private void setupSpinner() {
        try {
            String[] categories = {
                    "Nhân sự",
                    "Thiết bị",
                    "Vật liệu",
                    "Khác"
            };

            // Sử dụng Custom Adapter
            CategorySpinnerAdapter adapter = new CategorySpinnerAdapter(this, categories);

            spinnerCategory.setAdapter(adapter);
            spinnerCategory.setText(categories[0], false);

            // Set popup background
            spinnerCategory.setDropDownBackgroundResource(android.R.color.white);

            // Set click listener to log selection (for debugging)
            spinnerCategory.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String selectedItem = categories[position];
                    Log.d(TAG, "Selected category: " + selectedItem);
                }
            });

            Log.d(TAG, "Spinner được thiết lập với Custom Adapter");
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi thiết lập Spinner: " + e.getMessage(), e);
        }
    }

    /**
     * Setup sort dropdown for budgets
     */
    private void setupSortDropdown() {
        try {
            // Define sorting options
            String[] sortOptions = {
                    "Mặc định",
                    "Số tiền tăng dần",
                    "Số tiền giảm dần",
                    "Chưa duyệt trước",
                    "Đã duyệt trước"
            };

            // Create adapter
            ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    sortOptions
            );

            // Get dropdown component
            MaterialAutoCompleteTextView dropdownSort = findViewById(R.id.dropdown_sort_budget);
            if (dropdownSort != null) {
                dropdownSort.setAdapter(sortAdapter);

                // Set initial text
                dropdownSort.setText(sortOptions[0], false);

                // Handle item selection
                dropdownSort.setOnItemClickListener((parent, view, position, id) -> {
                    String selectedOption = sortOptions[position];
                    sortBudgets(selectedOption);
                });

                Log.d(TAG, "Sort dropdown setup successfully");
            } else {
                Log.e(TAG, "dropdown_sort_budget view not found");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up sort dropdown: " + e.getMessage(), e);
        }
    }

    /**
     * Sort budgets based on selected option
     */
    private void sortBudgets(String sortOption) {
        if (budgetList == null || budgetList.isEmpty()) return;

        try {
            // Convert string option to enum
            BudgetSorter.SortOption option = BudgetSorter.getSortOptionFromString(sortOption);

            // Apply sorting
            BudgetSorter.sortBudgets(budgetList, option);

            // Update the adapter
            budgetAdapter.notifyDataSetChanged();

            // Log the sorting operation
            Log.d(TAG, "Applied sorting: " + sortOption);
        } catch (Exception e) {
            Log.e(TAG, "Error during budget sorting: " + e.getMessage(), e);
        }
    }

    private void enablePagination() {
        isPaginationEnabled = true;
        budgetViewModel.enablePagination();
        if (btnLoadMore != null) {
            btnLoadMore.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Thiết lập sự kiện click
     */
    private void setupClickListeners() {
        try {
            // Click listener cho button Add Budget
            btnAddBudget.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "Button Add Budget clicked!");

                    // Debug info về form fields
                    String title = etBudgetTitle.getText() != null ? etBudgetTitle.getText().toString() : "";
                    String amount = etBudgetAmount.getText() != null ? etBudgetAmount.getText().toString() : "";
                    String description = etBudgetDescription.getText() != null ? etBudgetDescription.getText().toString() : "";
                    String category = spinnerCategory.getText() != null ? spinnerCategory.getText().toString() : "";

                    Log.d(TAG, "Form data - Title: " + title + ", Amount: " + amount + ", Description: " + description + ", Category: " + category);

                    if (editingBudgetId != null) {
                        Log.d(TAG, "Updating budget with ID: " + editingBudgetId);
                        updateBudget();
                    } else {
                        Log.d(TAG, "Adding new budget");
                        addNewBudget();
                    }
                }
            });

            // Click listener cho button Refresh
            btnRefresh.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "Button Refresh clicked!");
                    budgetViewModel.refreshBudgets();
                    Toast.makeText(BudgetActivity.this, "Làm mới dữ liệu", Toast.LENGTH_SHORT).show();
                }
            });

            // Click listener cho button Load More
            if (btnLoadMore != null) {
                btnLoadMore.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, "Button Load More clicked!");
                        budgetViewModel.loadMoreBudgets();
                    }
                });
            }

            // Test click trên description field
            etBudgetDescription.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "Description field clicked!");
                    etBudgetDescription.requestFocus();
                }
            });

            Log.d(TAG, "Click listeners setup completed");
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi thiết lập click listeners: " + e.getMessage(), e);
        }
    }

    /**
     * Quan sát dữ liệu từ ViewModel
     */
    private void observeViewModel() {
        // Quan sát danh sách ngân sách
        budgetViewModel.getBudgets().observe(this, budgets -> {
            if (budgets != null) {
                // Chuyển đổi từ List<Map> sang List<Budget>
                List<Budget> budgetObjects = convertMapListToBudgetList(budgets);

                // Store current sorting option if available
                MaterialAutoCompleteTextView dropdownSort = findViewById(R.id.dropdown_sort_budget);
                String currentSortOption = dropdownSort != null ? dropdownSort.getText().toString() : "Mặc định";

                // Update data
                budgetList.clear();
                budgetList.addAll(budgetObjects);

                // Apply current sorting
                sortBudgets(currentSortOption);

                // Update UI
                budgetAdapter.updateData(budgetList);

                // Cập nhật thống kê
                updateBudgetSummary();

                // Cập nhật biểu đồ
                setupBudgetPieChart();
                setupBudgetStatusBarChart();

                // Hiển thị số lượng
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setSubtitle("Tổng: " + budgets.size() + " khoản");
                }
            }
        });

        // Observe pagination-specific states
        budgetViewModel.getHasMoreBudgets().observe(this, hasMore -> {
            if (isPaginationEnabled && btnLoadMore != null) {
                btnLoadMore.setVisibility(hasMore ? View.VISIBLE : View.GONE);
                btnLoadMore.setEnabled(hasMore);
                if (hasMore) {
                    btnLoadMore.setText("Xem thêm");
                } else {
                    btnLoadMore.setText("Đã hết");
                    btnLoadMore.setEnabled(false);
                }
            }
        });

        // Quan sát thông báo lỗi
        budgetViewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(BudgetActivity.this, "Lỗi: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });

        // Quan sát trạng thái loading
        budgetViewModel.getIsLoading().observe(this, isLoading -> {
            if (progressBar != null) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
            // Disable load more button while loading
            if (isPaginationEnabled && btnLoadMore != null) {
                btnLoadMore.setEnabled(!isLoading && Boolean.TRUE.equals(budgetViewModel.getHasMoreBudgets().getValue()));
            }
        });
    }

    /**
     * Chuyển đổi từ List<Map> sang List<Budget>
     */
    private List<Budget> convertMapListToBudgetList(List<Map<String, Object>> mapList) {
        List<Budget> budgets = new ArrayList<>();
        for (Map<String, Object> map : mapList) {
            Budget budget = new Budget();
            budget.setId((String) map.get("id"));
            budget.setTitle((String) map.get("title"));
            budget.setDescription((String) map.get("description"));
            budget.setCategory((String) map.get("category"));
            budget.setUserId((String) map.get("userId"));

            // Xử lý amount
            Object amountObj = map.get("amount");
            if (amountObj instanceof Number) {
                budget.setAmount(((Number) amountObj).doubleValue());
            }

            // Xử lý approved
            Object approvedObj = map.get("approved");
            if (approvedObj instanceof Boolean) {
                budget.setApproved((Boolean) approvedObj);
            }

            // Xử lý date
            Object dateObj = map.get("createdAt");
            if (dateObj instanceof Date) {
                budget.setCreatedAt((Date) dateObj);
            }

            budgets.add(budget);
        }
        return budgets;
    }

    /**
     * Thêm khoản ngân sách mới
     */
    private void addNewBudget() {
        try {
            // An toàn khi lấy text từ EditText
            String title = getTextFromEditText(etBudgetTitle);
            String amountStr = getTextFromEditText(etBudgetAmount);
            String description = getTextFromEditText(etBudgetDescription);
            String category = getSelectedCategory();

            // Kiểm tra dữ liệu đầu vào
            if (TextUtils.isEmpty(title)) {
                showError(etBudgetTitle, "Vui lòng nhập tên chi phí");
                return;
            }

            if (TextUtils.isEmpty(amountStr)) {
                showError(etBudgetAmount, "Vui lòng nhập số tiền");
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    showError(etBudgetAmount, "Số tiền phải lớn hơn 0");
                    return;
                }
            } catch (NumberFormatException e) {
                showError(etBudgetAmount, "Số tiền không hợp lệ");
                return;
            }

            // Tạo budget mới
            Budget budget = new Budget(title, amount, description, category, null, false);
            Log.d(TAG, "Tạo ngân sách mới: " + title + " - " + amount + " VNĐ");

            // Thêm ngân sách thông qua ViewModel
            budgetViewModel.addBudget(budget);
            clearForm();
        } catch (Exception e) {
            Log.e(TAG, "Lỗi trong addNewBudget: " + e.getMessage(), e);
            Toast.makeText(this, "Có lỗi xảy ra: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Cập nhật budget đang chỉnh sửa
     */
    private void updateBudget() {
        try {
            if (editingBudgetId == null) return;

            String title = getTextFromEditText(etBudgetTitle);
            String amountStr = getTextFromEditText(etBudgetAmount);
            String description = getTextFromEditText(etBudgetDescription);
            String category = getSelectedCategory();

            // Validation tương tự như addNewBudget
            if (TextUtils.isEmpty(title)) {
                showError(etBudgetTitle, "Vui lòng nhập tên chi phí");
                return;
            }

            if (TextUtils.isEmpty(amountStr)) {
                showError(etBudgetAmount, "Vui lòng nhập số tiền");
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    showError(etBudgetAmount, "Số tiền phải lớn hơn 0");
                    return;
                }
            } catch (NumberFormatException e) {
                showError(etBudgetAmount, "Số tiền không hợp lệ");
                return;
            }

            // Tìm budget đang chỉnh sửa
            Budget editingBudget = null;
            for (Budget budget : budgetList) {
                if (budget.getId().equals(editingBudgetId)) {
                    editingBudget = budget;
                    break;
                }
            }

            if (editingBudget != null) {
                editingBudget.setTitle(title);
                editingBudget.setAmount(amount);
                editingBudget.setDescription(description);
                editingBudget.setCategory(category);

                // Update thông qua ViewModel
                budgetViewModel.updateBudget(editingBudget);

                // Reset editing state
                editingBudgetId = null;
                btnAddBudget.setText("Thêm Ngân sách");
                clearForm();

                Toast.makeText(this, "Cập nhật ngân sách thành công", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi trong updateBudget: " + e.getMessage(), e);
            Toast.makeText(this, "Có lỗi xảy ra: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * An toàn lấy text từ EditText
     */
    private String getTextFromEditText(TextInputEditText editText) {
        if (editText == null || editText.getText() == null) {
            return "";
        }
        return editText.getText().toString().trim();
    }

    /**
     * Hiển thị lỗi an toàn
     */
    private void showError(TextInputEditText editText, String message) {
        if (editText != null) {
            TextInputLayout inputLayout = (TextInputLayout) editText.getParent().getParent();
            if (inputLayout != null) {
                inputLayout.setError(message);
                inputLayout.requestFocus();
            } else {
                editText.setError(message);
                editText.requestFocus();
            }
        }
        Log.w(TAG, message);
    }

    /**
     * Lấy danh mục được chọn
     */
    private String getSelectedCategory() {
        try {
            String categoryText = spinnerCategory.getText().toString();

            if (categoryText.equals("Nhân sự")) {
                return "human_resource";
            } else if (categoryText.equals("Thiết bị")) {
                return "equipment";
            } else if (categoryText.equals("Vật liệu")) {
                return "material";
            } else {
                return "other";
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi lấy category: " + e.getMessage(), e);
        }
        return "other";
    }

    /**
     * Xóa form sau khi thêm
     */
    private void clearForm() {
        try {
            if (etBudgetTitle != null) etBudgetTitle.setText("");
            if (etBudgetAmount != null) etBudgetAmount.setText("");
            if (etBudgetDescription != null) etBudgetDescription.setText("");

            // Reset category spinner
            if (spinnerCategory != null) {
                spinnerCategory.setText("Nhân sự", false);
            }

            // Clear errors
            clearErrors();

            Log.d(TAG, "Form được xóa");
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi xóa form: " + e.getMessage(), e);
        }
    }

    /**
     * Xóa error messages
     */
    private void clearErrors() {
        if (etBudgetTitle != null) {
            TextInputLayout tilTitle = (TextInputLayout) etBudgetTitle.getParent().getParent();
            if (tilTitle != null) tilTitle.setError(null);
        }
        if (etBudgetAmount != null) {
            TextInputLayout tilAmount = (TextInputLayout) etBudgetAmount.getParent().getParent();
            if (tilAmount != null) tilAmount.setError(null);
        }
    }

    /**
     * Cập nhật thống kê ngân sách
     */
    private void updateBudgetSummary() {
        try {
            double totalBudget = 0;
            double approvedBudget = 0;

            if (budgetList != null) {
                for (Budget budget : budgetList) {
                    if (budget != null) {
                        totalBudget += budget.getAmount();
                        if (budget.isApproved()) {
                            approvedBudget += budget.getAmount();
                        }
                    }
                }
            }

            if (tvTotalBudget != null) {
                tvTotalBudget.setText("Tổng ngân sách: " + decimalFormat.format(totalBudget));
            }

            if (tvApprovedBudget != null) {
                tvApprovedBudget.setText("Đã duyệt: " + decimalFormat.format(approvedBudget));
            }

            Log.d(TAG, "Cập nhật thống kê - Tổng: " + totalBudget + ", Đã duyệt: " + approvedBudget);
        } catch (Exception e) {
            Log.e(TAG, "Lỗi trong updateBudgetSummary: " + e.getMessage(), e);
        }
    }

    /**
     * Xử lý click vào khoản ngân sách
     */
    private void handleBudgetClick(Budget budget) {
        try {
            if (budget == null) {
                Log.w(TAG, "Budget object is null");
                return;
            }

            String budgetId = budget.getId();
            boolean isApproved = budget.isApproved();

            Log.d(TAG, "Click vào ngân sách: " + budgetId + ", đã phê duyệt: " + isApproved);

            if (!isApproved && !TextUtils.isEmpty(budgetId)) {
                // Phê duyệt ngân sách thông qua ViewModel
                budgetViewModel.approveBudget(budgetId);
                Toast.makeText(this, "Đang duyệt ngân sách...", Toast.LENGTH_SHORT).show();
            } else {
                if (isApproved) {
                    Toast.makeText(this, "Ngân sách đã được duyệt", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Không thể phê duyệt ngân sách", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi trong handleBudgetClick: " + e.getMessage(), e);
            Toast.makeText(this, "Có lỗi xảy ra: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Xử lý long click vào khoản ngân sách
     */
    private void handleBudgetLongClick(Budget budget, int position) {
        try {
            if (budget == null) {
                Log.w(TAG, "Budget object is null in long click");
                return;
            }

            // Hiển thị dialog với các options
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Tùy chọn");

            String[] options;
            if (budget.isApproved()) {
                options = new String[]{"Xem chi tiết", "Hủy duyệt", "Xóa"};
            } else {
                options = new String[]{"Xem chi tiết", "Duyệt", "Chỉnh sửa", "Xóa"};
            }

            builder.setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: // Xem chi tiết
                        showBudgetDetails(budget);
                        break;
                    case 1: // Duyệt/Hủy duyệt
                        if (budget.isApproved()) {
                            budgetViewModel.revokeBudget(budget.getId());
                        } else {
                            budgetViewModel.approveBudget(budget.getId());
                        }
                        break;
                    case 2: // Chỉnh sửa (chỉ khi chưa duyệt)
                        if (!budget.isApproved()) {
                            editBudget(budget);
                        } else { // Hoặc Xóa (khi đã duyệt)
                            deleteBudget(budget, position);
                        }
                        break;
                    case 3: // Xóa
                        deleteBudget(budget, position);
                        break;
                }
            });

            builder.create().show();

        } catch (Exception e) {
            Log.e(TAG, "Lỗi trong handleBudgetLongClick: " + e.getMessage(), e);
            Toast.makeText(this, "Có lỗi xảy ra: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Hiển thị chi tiết budget
     */
    private void showBudgetDetails(Budget budget) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chi tiết ngân sách");

        StringBuilder details = new StringBuilder();
        details.append("Tên: ").append(budget.getTitle()).append("\n\n");
        details.append("Số tiền: ").append(decimalFormat.format(budget.getAmount())).append("\n");
        details.append("Danh mục: ").append(getCategoryDisplayName(budget.getCategory())).append("\n");
        details.append("Mô tả: ").append(budget.getDescription()).append("\n");
        details.append("Trạng thái: ").append(budget.isApproved() ? "Đã duyệt" : "Chờ duyệt").append("\n");

        if (budget.getCreatedAt() != null) {
            details.append("Ngày tạo: ").append(formatDate(budget.getCreatedAt()));
        }

        builder.setMessage(details.toString());
        builder.setPositiveButton("Đóng", null);
        builder.create().show();
    }

    /**
     * Chỉnh sửa budget
     */
    private void editBudget(Budget budget) {
        // Set editing state
        editingBudgetId = budget.getId();
        btnAddBudget.setText("Cập nhật Ngân sách");

        // Điền thông tin hiện tại vào form
        if (etBudgetTitle != null) {
            etBudgetTitle.setText(budget.getTitle());
        }
        if (etBudgetAmount != null) {
            etBudgetAmount.setText(String.valueOf(budget.getAmount()));
        }
        if (etBudgetDescription != null) {
            etBudgetDescription.setText(budget.getDescription());
        }
        if (spinnerCategory != null) {
            spinnerCategory.setText(getCategoryDisplayName(budget.getCategory()), false);
        }

        // Scroll lên form
        findViewById(R.id.btn_add_budget).requestFocus();

        Toast.makeText(this, "Thông tin đã được điền vào form. Chỉnh sửa và bấm 'Cập nhật'",
                Toast.LENGTH_LONG).show();
    }

    /**
     * Xóa budget
     */
    private void deleteBudget(Budget budget, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Xác nhận xóa");
        builder.setMessage("Bạn có chắc chắn muốn xóa khoản ngân sách này?");

        builder.setPositiveButton("Xóa", (dialog, which) -> {
            budgetViewModel.deleteBudget(budget.getId());
            Toast.makeText(BudgetActivity.this, "Đang xóa ngân sách...", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Hủy", null);
        builder.create().show();
    }

    /**
     * Chuyển đổi category key sang display name
     */
    private String getCategoryDisplayName(String category) {
        if (category == null) return "Khác";

        switch (category) {
            case "human_resource":
                return "Nhân sự";
            case "equipment":
                return "Thiết bị";
            case "material":
                return "Vật liệu";
            default:
                return "Khác";
        }
    }

    /**
     * Format date theo kiểu Việt Nam
     */
    private String formatDate(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return formatter.format(date);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cleanup
        budgetList = null;
        budgetAdapter = null;
        Log.d(TAG, "BudgetActivity destroyed");
    }
}