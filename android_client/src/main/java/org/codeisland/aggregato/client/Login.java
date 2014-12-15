package org.codeisland.aggregato.client;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import org.codeisland.aggregato.client.network.Endpoint;

/**
 * @author Lukas Knuth
 * @version 1.0
 */
public class Login extends Activity implements View.OnClickListener {

    public static final String ACCOUNT_PICKER_KEY = "AccountPicker";
    private static final int REQUEST_ACCOUNT_PICKER = 2;

    private Button login_button;
    private Intent account_picker_intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.login_page);

        account_picker_intent = this.getIntent().getParcelableExtra(ACCOUNT_PICKER_KEY);
        assert account_picker_intent != null;

        this.login_button = (Button) findViewById(R.id.login_login_button);
        login_button.setOnClickListener(this);
    }

    @Override
    public void onClick(View source) {
        if (source == login_button){
            this.startActivityForResult(account_picker_intent, REQUEST_ACCOUNT_PICKER);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ACCOUNT_PICKER && data != null && data.getExtras() != null){
            String account_name = data.getExtras().getString(AccountManager.KEY_ACCOUNT_NAME);
            if (account_name != null){
                Endpoint.setSelectedAccountName(account_name, this);
                setResult(RESULT_OK);
            } else {
                setResult(RESULT_CANCELED);
            }
        } else {
            setResult(RESULT_CANCELED);
        }

        finish();
    }
}
