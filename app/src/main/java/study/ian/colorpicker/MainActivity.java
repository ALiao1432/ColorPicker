package study.ian.colorpicker;

import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import study.ian.colorpickerlib.ColorPickerView;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity";

    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViews();
        setViews();

        createDialog();
    }

    private void findViews() {
        button = findViewById(R.id.btn_select);
    }

    private void setViews() {
        button.setOnClickListener(v -> createDialog());
    }

    private void createDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_view, null);
        ColorPickerView colorPickerView = view.findViewById(R.id.colorPickerView);
        colorPickerView.setColorSelectListener(color ->
                Log.d(TAG, "colorSelectListener color : " + color));

        new AlertDialog.Builder(this)
                .setView(view)
                .setPositiveButton(R.string.btn_check, (dialog, which) -> {
                    int color = colorPickerView.getSelectColor();
                    button.setBackgroundColor(color);
                })
                .show();
    }
}
