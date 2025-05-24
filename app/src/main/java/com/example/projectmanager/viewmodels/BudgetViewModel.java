package com.example.projectmanager.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.projectmanager.models.Budget;
import com.example.projectmanager.repositories.BudgetRepository;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ViewModel cho Budget Management với pagination
 */
public class BudgetViewModel extends ViewModel {
    private static final String TAG = "BudgetViewModel";

    private BudgetRepository budgetRepository;
    private MutableLiveData<List<Map<String, Object>>> budgets;
    private MutableLiveData<String> errorMessage;
    private MutableLiveData<Boolean> isLoading;
    private MutableLiveData<Boolean> hasMoreBudgets;

    // Pagination state
    private DocumentSnapshot lastBudgetDocument = null;
    private boolean isPaginationMode = false;

    public BudgetViewModel() {
        budgetRepository = new BudgetRepository();
        budgets = new MutableLiveData<>();
        errorMessage = new MutableLiveData<>();
        isLoading = new MutableLiveData<>(false);
        hasMoreBudgets = new MutableLiveData<>(false);
    }

    // Public methods for the Activity to observe
    public LiveData<List<Map<String, Object>>> getBudgets() {
        return budgets;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<Boolean> getHasMoreBudgets() {
        return hasMoreBudgets;
    }

    /**
     * Enable pagination mode
     */
    public void enablePagination() {
        isPaginationMode = true;
        hasMoreBudgets.setValue(false);
        lastBudgetDocument = null;
        loadBudgetsWithPagination();
    }

    /**
     * Load budgets with pagination
     */
    public void loadBudgetsWithPagination() {
        isLoading.setValue(true);
        budgetRepository.getBudgetsWithPagination(lastBudgetDocument,
                new BudgetRepository.OnPagedBudgetsLoadedListener() {
                    @Override
                    public void onBudgetsLoaded(List<Map<String, Object>> newBudgets, boolean hasMore, DocumentSnapshot lastDocument) {
                        handlePaginatedBudgetsLoaded(newBudgets, hasMore, lastDocument);
                    }

                    @Override
                    public void onError(String error) {
                        errorMessage.setValue(error);
                        isLoading.setValue(false);
                    }
                });
    }

    /**
     * Load more budgets (for pagination)
     */
    public void loadMoreBudgets() {
        if (!isPaginationMode || Boolean.FALSE.equals(hasMoreBudgets.getValue()) || lastBudgetDocument == null) {
            return;
        }

        isLoading.setValue(true);
        budgetRepository.getBudgetsWithPagination(lastBudgetDocument,
                new BudgetRepository.OnPagedBudgetsLoadedListener() {
                    @Override
                    public void onBudgetsLoaded(List<Map<String, Object>> newBudgets, boolean hasMore, DocumentSnapshot lastDocument) {
                        handlePaginatedBudgetsLoaded(newBudgets, hasMore, lastDocument);
                    }

                    @Override
                    public void onError(String error) {
                        errorMessage.setValue(error);
                        isLoading.setValue(false);
                    }
                });
    }

    private void handlePaginatedBudgetsLoaded(List<Map<String, Object>> newBudgets, boolean hasMore, DocumentSnapshot lastDocument) {
        isLoading.setValue(false);

        List<Map<String, Object>> currentBudgets = budgets.getValue();
        if (currentBudgets == null) {
            currentBudgets = new ArrayList<>();
        }

        if (lastBudgetDocument == null) {
            // First load
            currentBudgets.clear();
        }

        currentBudgets.addAll(newBudgets);
        budgets.setValue(currentBudgets);
        hasMoreBudgets.setValue(hasMore);
        lastBudgetDocument = lastDocument;
    }

    /**
     * Load all budgets (non-pagination mode)
     */
    public void loadBudgets() {
        isLoading.setValue(true);
        budgetRepository.getAllBudgets(new BudgetRepository.OnBudgetsLoadedListener() {
            @Override
            public void onBudgetsLoaded(List<Map<String, Object>> budgetList) {
                budgets.setValue(budgetList);
                isLoading.setValue(false);
            }

            @Override
            public void onError(String error) {
                errorMessage.setValue(error);
                isLoading.setValue(false);
            }
        });
    }

    /**
     * Refresh budgets (reset pagination if enabled)
     */
    public void refreshBudgets() {
        if (isPaginationMode) {
            lastBudgetDocument = null;
            loadBudgetsWithPagination();
        } else {
            loadBudgets();
        }
    }

    /**
     * Add new budget
     */
    public void addBudget(Budget budget) {
        isLoading.setValue(true);
        budgetRepository.addBudget(budget, new BudgetRepository.OnBudgetOperationListener() {
            @Override
            public void onSuccess(String message) {
                isLoading.setValue(false);
                // In pagination mode, refresh to show new budget at the top
                if (isPaginationMode) {
                    refreshBudgets();
                } else {
                    loadBudgets();
                }
            }

            @Override
            public void onError(String error) {
                errorMessage.setValue(error);
                isLoading.setValue(false);
            }
        });
    }

    /**
     * Update existing budget
     */
    public void updateBudget(Budget budget) {
        isLoading.setValue(true);
        budget.setUpdatedAt(new java.util.Date()); // Set update timestamp
        budgetRepository.updateBudget(budget, new BudgetRepository.OnBudgetOperationListener() {
            @Override
            public void onSuccess(String message) {
                isLoading.setValue(false);
                // Reload data after successful update
                if (isPaginationMode) {
                    refreshBudgets();
                } else {
                    loadBudgets();
                }
            }

            @Override
            public void onError(String error) {
                errorMessage.setValue(error);
                isLoading.setValue(false);
            }
        });
    }

    /**
     * Approve a budget
     */
    public void approveBudget(String budgetId) {
        isLoading.setValue(true);
        budgetRepository.approveBudget(budgetId, new BudgetRepository.OnBudgetOperationListener() {
            @Override
            public void onSuccess(String message) {
                isLoading.setValue(false);
                // Reload data after successful approval
                if (isPaginationMode) {
                    refreshBudgets();
                } else {
                    loadBudgets();
                }
            }

            @Override
            public void onError(String error) {
                errorMessage.setValue(error);
                isLoading.setValue(false);
            }
        });
    }

    /**
     * Revoke approval of a budget
     */
    public void revokeBudget(String budgetId) {
        isLoading.setValue(true);
        budgetRepository.revokeBudget(budgetId, new BudgetRepository.OnBudgetOperationListener() {
            @Override
            public void onSuccess(String message) {
                isLoading.setValue(false);
                // Reload data after successful revocation
                if (isPaginationMode) {
                    refreshBudgets();
                } else {
                    loadBudgets();
                }
            }

            @Override
            public void onError(String error) {
                errorMessage.setValue(error);
                isLoading.setValue(false);
            }
        });
    }

    /**
     * Delete a budget
     */
    public void deleteBudget(String budgetId) {
        isLoading.setValue(true);
        budgetRepository.deleteBudget(budgetId, new BudgetRepository.OnBudgetOperationListener() {
            @Override
            public void onSuccess(String message) {
                isLoading.setValue(false);
                // Reload data after successful deletion
                if (isPaginationMode) {
                    refreshBudgets();
                } else {
                    loadBudgets();
                }
            }

            @Override
            public void onError(String error) {
                errorMessage.setValue(error);
                isLoading.setValue(false);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Clean up if needed
    }
}