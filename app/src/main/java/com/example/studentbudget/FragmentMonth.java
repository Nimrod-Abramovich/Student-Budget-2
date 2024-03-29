package com.example.studentbudget;


import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.text.DateFormatSymbols;

public class FragmentMonth extends Fragment {

    View view;
    MySharedPreferences sp;
    DatabaseHelper db;
    float budget;
    float expenses;
    String month;
    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");

    final int LARGEST_EXPENSES_COUNT = 3;
    String[] expenseName = new String[LARGEST_EXPENSES_COUNT];
    String[] expenseCategoryColour = new String[LARGEST_EXPENSES_COUNT];
    String[] expenseCategoryName = new String[LARGEST_EXPENSES_COUNT];
    String[] expensePrice = new String[LARGEST_EXPENSES_COUNT];
    String[] expenseDate = new String[LARGEST_EXPENSES_COUNT];

    SimpleDateFormat startSDF = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
    SimpleDateFormat endSDF = new SimpleDateFormat("dd/MM/yy");

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_month, container, false);
        operations();
        return view;
    }

    private void operations() {
        sp = new MySharedPreferences(getActivity(), MySharedPreferences.PREFERENCE_MONTH_KEY);
        db = new DatabaseHelper(getActivity());
        budget = getBudget();
        expenses = getExpenses();
        month = getMonth();
        initialiseDefaultData();
        getLargestExpenses();
        setupListAdapter();
        showData();
        editBudgetEvent();
        viewBudgetHistoryEvent();
    }

    private float getBudget() {
        return sp.readPreferenceData(MySharedPreferences.KEY_BUDGET);

    }

    private float getExpenses() {
        return sp.readPreferenceData(MySharedPreferences.KEY_EXPENSES);
    }

    private String getMonth() {
        Calendar monthNumberCalendar = Calendar.getInstance();
        monthNumberCalendar.setTime(new Date());
        return new DateFormatSymbols().getMonths()[monthNumberCalendar.get(Calendar.MONTH)];
    }

    private void initialiseDefaultData() {
        for (int i = 0; i < LARGEST_EXPENSES_COUNT; ++i) {
            expenseCategoryColour[i] = "";
            expenseName[i] = "None";
            expensePrice[i] = "";
            expenseCategoryName[i] = "";
            expenseDate[i] = "";
        }
    }

    private void getLargestExpenses() {
        String dateToday = sdf.format(new Date());
        String[] dateTodayArray = dateToday.split("/");
        String month = "01/" + dateTodayArray[1] + "/" + dateTodayArray[2];
        int nextMonthNum = Integer.parseInt(dateTodayArray[1]) + 1;
        String nextMonth = "01/" + nextMonthNum + "/" + dateTodayArray[2];

        String query = "select * from " + DatabaseHelper.TABLE_EXPENSES + " order by " + DatabaseHelper.COL_PRICE + " desc;";
        Cursor expenseData = db.myQuery(query);

        int count = 0;
        for (int i = 0; i < expenseData.getCount(); ++i) {
            expenseData.moveToPosition(i);
            Date date;
            try {
                date =  startSDF.parse(expenseData.getString(4));
                if (date.compareTo(endSDF.parse(month)) >= 0 && date.compareTo(endSDF.parse(nextMonth)) < 0) {
                    getCategoryData(getExpenseData(expenseData, count), count);
                    ++count;
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
            if (count == 3)
                return;
        }
    }

    private int getExpenseData(Cursor expenseData, int i) {
        expenseName[i] = expenseData.getString(1);
        expensePrice[i] = "£" + expenseData.getFloat(2);
        try {
            expenseDate[i] = endSDF.format(startSDF.parse(expenseData.getString(4)));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return expenseData.getInt(3);
    }

    private void getCategoryData(int categoryId, int i) {
        Cursor categoryData = db.searchData(DatabaseHelper.TABLE_CATEGORIES, "*", DatabaseHelper.COL_ID, categoryId);
        categoryData.moveToFirst();
        expenseCategoryName[i] = categoryData.getString(1);
        expenseCategoryColour[i] = categoryData.getString(2);
    }

    private void setupListAdapter() {
        ListView largestExpensesListView = view.findViewById(R.id.monthLargestExpensesListView);
        ExpensesListAdapter expensesListAdapter = new ExpensesListAdapter(getActivity(), expenseCategoryColour, expenseName, expenseCategoryName, expensePrice, expenseDate);
        largestExpensesListView.setAdapter(expensesListAdapter);
    }

    private void showData() {
        TextView monthHeadingTextView = view.findViewById(R.id.monthBudgetTabHeadingTextView);
        ProgressBar budgetProgressBar = view.findViewById(R.id.monthBudgetPBar);
        TextView budgetPercentageTextView = view.findViewById(R.id.monthBudgetPercentageTextView);
        TextView expensesValueTextView = view.findViewById(R.id.monthExpensesValueTextView);
        TextView budgetValueTextView = view.findViewById(R.id.monthBudgetValueTextView);

        monthHeadingTextView.setText(month);
        float budgetPercentage = expenses * 100 / budget;
        budgetProgressBar.setProgress(Math.round(budgetPercentage));
        budgetPercentageTextView.setText(Math.round(10 * budgetPercentage) / 10 + "%");
        expensesValueTextView.setText("£" + expenses);
        budgetValueTextView.setText("£" + budget);
    }

    private void editBudgetEvent() {
        Button editBudgetButton = view.findViewById(R.id.monthEditBudgetButton);
        editBudgetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), EditBudgetActivity.class);
                intent.putExtra("EDIT_BUDGET_TYPE", "MONTH");
                startActivity(intent);
            }
        });
    }

    private void viewBudgetHistoryEvent() {
        Cursor data = db.searchData(DatabaseHelper.TABLE_MONTHLY_BUDGET_HISTORY, "*");
        final int count = data.getCount();
        Button viewBudgetHistoryButton = view.findViewById(R.id.monthlyBudgetHistoryButton);
        viewBudgetHistoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (count == 1) {
                    Toast.makeText(getActivity(), "No monthly history data found.", Toast.LENGTH_LONG).show();
                }
                else {
                    Intent intent = new Intent(getActivity(), BudgetHistoryActivity.class);
                    intent.putExtra("BUDGET_HISTORY_TYPE", "MONTH");
                    startActivity(intent);
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        budget = getBudget();
        expenses = getExpenses();
        month = getMonth();
        initialiseDefaultData();
        getLargestExpenses();
        setupListAdapter();
        showData();
    }
}
