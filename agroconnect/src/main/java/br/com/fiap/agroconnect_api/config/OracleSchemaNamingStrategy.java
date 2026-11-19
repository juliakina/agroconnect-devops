package br.com.fiap.agroconnect_api.config;

import org.hibernate.boot.model.naming.Identifier;
import java.util.Locale;
import org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

public class OracleSchemaNamingStrategy extends PhysicalNamingStrategySnakeCaseImpl {

	@Override
	public Identifier toPhysicalSchemaName(Identifier name, JdbcEnvironment environment) {
		if (name != null && "agroconnect".equalsIgnoreCase(name.getText())) return null;
		return super.toPhysicalSchemaName(name, environment);
	}

	@Override
	public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment environment) {
		return name != null && name.isQuoted() ? Identifier.toIdentifier(name.getText().toUpperCase(Locale.ROOT))
				: super.toPhysicalTableName(name, environment);
	}

	@Override
	public Identifier toPhysicalColumnName(Identifier name, JdbcEnvironment environment) {
		return name != null && name.isQuoted() ? Identifier.toIdentifier(name.getText().toUpperCase(Locale.ROOT))
				: super.toPhysicalColumnName(name, environment);
	}

}
