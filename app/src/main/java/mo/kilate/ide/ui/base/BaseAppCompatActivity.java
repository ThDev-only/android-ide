package mo.kilate.ide.ui.base;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.io.Serializable;
import mo.kilate.ide.R;
import mo.kilate.ide.os.PermissionManager;
import mo.kilate.ide.os.PermissionStatus;
import mo.kilate.ide.os.PermissionType;
import mo.kilate.ide.ui.components.dialog.ProgressDialog;
import mo.kilate.ide.utils.PrintUtil;
import mo.kilate.ide.utils.StringUtil;

@SuppressWarnings("DEPRECATION")
public abstract class BaseAppCompatActivity extends AppCompatActivity {

  public static final int TOAST_LENGHT = 4000;

  @NonNull private View rootView;
  @NonNull private ProgressDialog progressDialog;
  @NonNull protected PermissionManager.Storage storagePermissionManager;

  private final ActivityResultLauncher<Intent> allFilesPermissionLauncher =
      registerForActivityResult(
          new ActivityResultContracts.StartActivityForResult(),
          result -> {
            onReceive(PermissionType.STORAGE, storagePermissionManager.check());
          });

  private final ActivityResultLauncher<String[]> readWritePermissionLauncher =
      registerForActivityResult(
          new ActivityResultContracts.RequestMultiplePermissions(),
          permissions -> {
            onReceive(PermissionType.STORAGE, storagePermissionManager.check());
          });

  @Override
  protected void onCreate(@Nullable final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    rootView = bindLayout();
    setContentView(rootView);
    onBindLayout(savedInstanceState);
    storagePermissionManager =
        new PermissionManager.Storage(
            this, allFilesPermissionLauncher, readWritePermissionLauncher);
    progressDialog = new ProgressDialog(this);
    onPostBind(savedInstanceState);
  }

  @NonNull
  protected abstract View bindLayout();

  protected abstract void onBindLayout(@Nullable final Bundle savedInstanceState);

  protected void onPostBind(@Nullable final Bundle savedInstanceState) {
    if (storagePermissionManager.check() == PermissionStatus.DENIED)
      storagePermissionManager.request();
    // EdgeToEdge.enable(this);
  }

  protected void showProgress() {
    if (progressDialog != null && !progressDialog.isShowing() && !isFinishing()) {
      progressDialog.show();
    }
  }

  protected void showProgress(@NonNull final String text) {
    if (progressDialog != null && !progressDialog.isShowing() && !isFinishing()) {
      progressDialog.setTitle(text);
      progressDialog.show();
    }
  }

  protected void dismissProgress() {
    try {
      if (progressDialog != null && progressDialog.isShowing()) {
        progressDialog.getIndicator().setProgress(0, true);
        progressDialog.dismiss();
      }
    } catch (final Exception e) {
      progressDialog = null;
      progressDialog = new ProgressDialog(this);
      PrintUtil.print(e);
    }
  }

  protected ProgressDialog getProgressDialog() {
    return progressDialog;
  }

  /**
   * Called when user grant or deny an permission.
   *
   * @param type The Type of Permission. see enum PermissionType.
   * @param status The status of permission. see enum PermissionStatus.
   */
  protected void onReceive(final PermissionType type, final PermissionStatus status) {}

  public View getRootView() {
    return rootView;
  }

  protected void configureToolbar(@NonNull MaterialToolbar toolbar) {
    setSupportActionBar(toolbar);
    toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
  }

  protected final void toast(final String message) {
    Toast.makeText(this, message, TOAST_LENGHT).show();
  }

  protected final void openActivity(final Class<? extends BaseAppCompatActivity> activity) {
    startActivity(new Intent(this, activity));
  }

  protected AlertDialog openSimplePopup(
      final String title, final String message, final Runnable ok) {
    var dialog =
        new MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(
                StringUtil.getString(R.string.common_word_ok),
                (d, w) -> {
                  ok.run();
                  d.dismiss();
                })
            .create();
    dialog.show();
    return dialog;
  }

  protected final void doWithProgress(final Runnable first) {
    doWithProgress(() -> {}, first);
  }

  protected final void doWithProgress(final Runnable first, final Runnable second) {
    doWithProgress(() -> {}, first, second);
  }

  protected final void doWithProgress(
      final Runnable first, final Runnable second, final Runnable third) {
    new Thread(
            () -> {
              try {
                first.run();
                Thread.sleep(250);
                second.run();
                Thread.sleep(250);
                third.run();
              } catch (InterruptedException e) {
                openSimplePopup(StringUtil.getString(R.string.text_error), e.toString(), () -> {});
              }
            })
        .start();
  }

  @Nullable
  protected <T extends Serializable> T getSerializable(final String key, final Class<T> clazz) {
    var extras = getIntent().getExtras();
    return getSerializable(extras, key, clazz);
  }

  @Nullable
  protected <T extends Serializable> T getSerializable(
      final Bundle bundle, final String key, final Class<T> clazz) {
    if (bundle == null) return null;
    if (!bundle.containsKey(key)) return null;

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      return bundle.getSerializable(key, clazz);
    } else {
      return clazz.cast(bundle.getSerializable(key));
    }
  }

  @Nullable
  protected <T extends Parcelable> T getParcelable(final String key, final Class<T> clazz) {
    var extras = getIntent().getExtras();
    return getParcelable(extras, key, clazz);
  }

  @Nullable
  protected <T extends Parcelable> T getParcelable(
      final Bundle bundle, final String key, final Class<T> clazz) {
    if (bundle == null) return null;
    if (!bundle.containsKey(key)) return null;

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      return bundle.getParcelable(key, clazz);
    } else {
      return clazz.cast(bundle.getParcelable(key));
    }
  }
}
