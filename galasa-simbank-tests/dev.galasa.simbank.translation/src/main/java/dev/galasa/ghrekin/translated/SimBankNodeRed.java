package dev.galasa.ghrekin.translated;

import dev.galasa.simbank.manager.ISimBank;
import dev.galasa.http.IHttpClient;
import java.net.URISyntaxException;
import dev.galasa.artifact.IArtifactManager;
import java.util.HashMap;
import java.io.IOException;
import dev.galasa.artifact.TestBundleResourceException;
import dev.galasa.simbank.manager.IAccount;
import dev.galasa.http.HttpClientException;
import static org.assertj.core.api.Assertions.assertThat;
import java.math.BigDecimal;
import dev.galasa.simbank.manager.SimBankManagerException;
import dev.galasa.simbank.manager.SimBank;
import dev.galasa.artifact.IBundleResources;
import dev.galasa.Test;
import dev.galasa.artifact.ArtifactManager;
import java.net.URI;
import dev.galasa.simbank.manager.Account;
import dev.galasa.http.HttpClient;
import java.io.InputStream;

@Test
public class SimBankNodeRed {
	//Obtain account with 500
	@Account(balance = "500")
	public IAccount account1;

	@SimBank
	public ISimBank bank;

	@ArtifactManager
	public IArtifactManager artifacts;

	@HttpClient
	public IHttpClient client;

	//Obtain account with 500
	@Account(balance = "500")
	public IAccount account2;

	//Obtain account with 2000
	@Account(balance = "2000")
	public IAccount account3;

	@Test
	public void method0() throws TestBundleResourceException, IOException, URISyntaxException, HttpClientException, SimBankManagerException {
		//Credit with 500
		client.build();
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("ACCOUNT_NUMBER", account1.getAccountNumber());
		parameters.put("AMOUNT", "500");
		IBundleResources resources = artifacts.getBundleResources(this.getClass());
		InputStream is = resources.retrieveSkeletonFile("/resources/skeletons/testSkel.skel", parameters);
		String textContent = resources.streamAsString(is);
		client.setURI(new URI(bank.getFullAddress()));
		client.postTextAsXML(bank.getUpdateAddress(), textContent, false);

		//Check account is 1000
		assertThat(account1.getBalance()).isEqualByComparingTo(new BigDecimal("1000"));
	}

	@Test
	public void method1() throws TestBundleResourceException, IOException, URISyntaxException, HttpClientException, SimBankManagerException {
		//Credit with 500
		client.build();
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("ACCOUNT_NUMBER", account2.getAccountNumber());
		parameters.put("AMOUNT", "500");
		IBundleResources resources = artifacts.getBundleResources(this.getClass());
		InputStream is = resources.retrieveSkeletonFile("/resources/skeletons/testSkel.skel", parameters);
		String textContent = resources.streamAsString(is);
		client.setURI(new URI(bank.getFullAddress()));
		client.postTextAsXML(bank.getUpdateAddress(), textContent, false);

		//Check account is 1000
		assertThat(account2.getBalance()).isEqualByComparingTo(new BigDecimal("1000"));
	}

	@Test
	public void method2() throws TestBundleResourceException, IOException, URISyntaxException, HttpClientException, SimBankManagerException {
		//Credit with 200
		client.build();
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("ACCOUNT_NUMBER", account3.getAccountNumber());
		parameters.put("AMOUNT", "200");
		IBundleResources resources = artifacts.getBundleResources(this.getClass());
		InputStream is = resources.retrieveSkeletonFile("/resources/skeletons/testSkel.skel", parameters);
		String textContent = resources.streamAsString(is);
		client.setURI(new URI(bank.getFullAddress()));
		client.postTextAsXML(bank.getUpdateAddress(), textContent, false);

		//Check account is 2200
		assertThat(account3.getBalance()).isEqualByComparingTo(new BigDecimal("2200"));
	}

}