package de.unihd.dbs.geoparser.util.dbconnectors;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import de.unihd.dbs.geoparser.util.dbconnectors.SSHConnectionData.SSHPortForwardingRule;
import de.unihd.dbs.geoparser.util.dbconnectors.SSHConnectionData.SSHPortForwardingRule.SSHPortForwardingDirection;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

public class SSHConnectionDataTest {

	private static final String sshHostAddress = "ssh_host_address";
	private static final String sshUserName = "ssh_username";
	private static final char[] sshPassword = "ssh_password".toCharArray();
	private static final int sshPort = 1234;
	private static final boolean sshRequired = true;

	private static final String sshPortFordwardingBindAddress = "*";
	private static final int sshPortForwardingLocalPort = 1234;
	private static final String sshPortForwardingHostAddress = "localhost";
	private static final int sshPortForwardingRemotePort = 5678;
	private static final SSHPortForwardingDirection sshPortForwardingDirection = SSHPortForwardingDirection.LOCAL;
	private static final SSHPortForwardingRule sshPortForwardingRule = new SSHPortForwardingRule(
			sshPortFordwardingBindAddress, sshPortForwardingLocalPort, sshPortForwardingHostAddress,
			sshPortForwardingRemotePort, sshPortForwardingDirection);
	private static final Set<SSHPortForwardingRule> sshPortForwardingRules = new HashSet<>(
			Arrays.asList(sshPortForwardingRule));
	private static final Hashtable<String, String> sshConfig = new Hashtable<>();

	static {
		sshConfig.put("string", "string_value");
	}

	private SSHConnectionData sshConnectionData;

	@Before
	public void createSSHConnectionData() {
		sshConnectionData = new SSHConnectionData(sshHostAddress, sshPort, sshUserName, sshPassword,
				sshPortForwardingRules, sshRequired, sshConfig);
	}

	@Test
	public void testSshPortForwardingRuleEqualsContractShouldHold() {
		EqualsVerifier.forClass(SSHPortForwardingRule.class).suppress(Warning.NONFINAL_FIELDS).verify();
	}

	@Test
	public void testSSHConnectionDataEqualsContractShouldHold() {
		EqualsVerifier.forClass(SSHConnectionData.class).suppress(Warning.NONFINAL_FIELDS).verify();
	}

	@Test
	public void testToStringShouldWork() {
		// perform a simple smoke test
		sshConnectionData.toString();
	}

	@Test
	public void testSSHConnectionDataConstructorShouldSetFieldsCorrectly() {
		assertThat(sshConnectionData.hostAddress, equalTo(sshHostAddress));
		assertThat(sshConnectionData.userName, equalTo(sshUserName));
		assertThat(sshConnectionData.password, equalTo(sshPassword));
		assertThat(sshConnectionData.port, equalTo(sshPort));
		assertThat(sshConnectionData.portForwardingRules, equalTo(sshPortForwardingRules));
		assertThat(sshConnectionData.sshRequired, equalTo(sshRequired));
	}

	@Test
	public void testSSHConnectionDataConstructorShouldSetNullPasswordCorrectly() {
		sshConnectionData = new SSHConnectionData(sshHostAddress, sshPort, sshUserName, null, sshPortForwardingRules,
				sshRequired, sshConfig);
		assertThat(sshConnectionData.password, nullValue());
	}

}
