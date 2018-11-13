package study.ian.colorpicker;

import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import study.ian.colorpickerlib.ColorPickerView;
import study.ian.colorpickerlib.OnColorSelectListener;

public class MainActivity extends AppCompatActivity implements OnColorSelectListener {

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

    @Override
    public void onSelectColor(int color) {
        Log.d(TAG, "onSelectColor: color : " + color);
    }

    private void findViews() {
        button = findViewById(R.id.btn_select);
    }

    private void setViews() {
        button.setOnClickListener(v -> createDialog());
    }

    private void createDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_view, null);

        new AlertDialog.Builder(this)
                .setView(view)
                .setPositiveButton(R.string.btn_check, (dialog, which) -> {
                    ColorPickerView colorPickerView = view.findViewById(R.id.colorPickerView);
                    int color = colorPickerView.getSelectColor();
                    button.setBackgroundColor(color);
                })
                .show();
    }
}
