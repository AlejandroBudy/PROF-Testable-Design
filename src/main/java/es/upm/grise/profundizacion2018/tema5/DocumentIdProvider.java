package es.upm.grise.profundizacion2018.tema5;

import static es.upm.grise.profundizacion2018.tema5.Error.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class DocumentIdProvider {

	// Environment variable
	private static String ENVIRON  = "APP_HOME";

	// ID for the newly created documents
	private int documentId;

	// Connection to database (open during program execution)
	Connection connection = null;

	// Singleton access
	private static DocumentIdProvider instance;
	public static final String UPDATE_COUNTERS_SET_DOCUMENT_ID = "UPDATE Counters SET documentId = ?";
	public static final String SELECT_DOCUMENT_ID_FROM_COUNTERS = "SELECT documentId FROM Counters";

	public static DocumentIdProvider getInstance() throws NonRecoverableError {
		if (instance != null)

			return instance;

		else {

			instance = new DocumentIdProvider();
			return instance;

		}
	}

	// Create the connection to the database
	DocumentIdProvider() throws NonRecoverableError {
		Properties properties = getProperties();
		initClass(properties);
	}

	void initClass(Properties properties) throws NonRecoverableError {

		prepareDriverDB();
		createDBConnection(properties);
		getCurrentDocumentId();
	}

	private Properties getProperties() throws NonRecoverableError {
		String path = getPath();


		Properties propertiesInFile = new Properties();

		setInputFileInProperties(path, propertiesInFile);
		return propertiesInFile;
	}

	String getPath() throws NonRecoverableError {
	   String path = System.getenv(ENVIRON);
		if (path == null) {

			System.out.println(UNDEFINED_ENVIRON.getMessage());
			throw new NonRecoverableError();

		}
		return path;
	}

	private void createDBConnection(Properties properties) throws NonRecoverableError {

		String url = properties.getProperty("url");
		String username = properties.getProperty("username");
		String password = properties.getProperty("password");

		try {

			connection = DriverManager.getConnection(url, username, password);

		} catch (SQLException e) {

			System.out.println(CANNOT_CONNECT_DATABASE.getMessage());
			throw new NonRecoverableError();

		}
	}

	private void getCurrentDocumentId() throws NonRecoverableError {

		Statement statement = null;
		ResultSet resultSet = null;

		try {

			statement = connection.createStatement();
			resultSet = statement.executeQuery(SELECT_DOCUMENT_ID_FROM_COUNTERS);

		} catch (SQLException e) {

			System.out.println(CANNOT_RUN_QUERY.getMessage());
			throw new NonRecoverableError();

		}

		// Get the last objectID
			int numberOfValues = 0;
		try {

			while (resultSet.next()) {

				documentId = resultSet.getInt("documentId");
				numberOfValues++;

			}

		} catch (SQLException e) {

			System.out.println(INCORRECT_COUNTER.getMessage());
			throw new NonRecoverableError();

		}

		// Only one objectID can be retrieved
		if(numberOfValues != 1) {

			System.out.println(CORRUPTED_COUNTER.getMessage());
			throw new NonRecoverableError();

		}

		// Close all DB connections
		try {

			resultSet.close();
			statement.close();

		} catch (SQLException e) {

			System.out.println(CONNECTION_LOST.getMessage());
			throw new NonRecoverableError();

		}
	}



	private void prepareDriverDB() throws NonRecoverableError {
		// Load DB driver
		try {

			Class.forName("com.mysql.jdbc.Driver").newInstance();

		} catch (InstantiationException e) {

			System.out.println(CANNOT_INSTANTIATE_DRIVER.getMessage());
			throw new NonRecoverableError();

		} catch (IllegalAccessException e) {

			System.out.println(CANNOT_INSTANTIATE_DRIVER.getMessage());
			throw new NonRecoverableError();

		} catch (ClassNotFoundException e) {

			System.out.println(CANNOT_FIND_DRIVER.getMessage());
			throw new NonRecoverableError();

		}
	}

	private void setInputFileInProperties(String path, Properties propertiesInFile) throws NonRecoverableError {
		InputStream inputFile = null;

		// Load the property file
		try {
			inputFile = new FileInputStream(path + "config.properties");
			propertiesInFile.load(inputFile);

		} catch (FileNotFoundException e) {

			System.out.println(NON_EXISTING_FILE.getMessage());
			throw new NonRecoverableError();

		} catch (IOException e) {

			System.out.println(CANNOT_READ_FILE.getMessage());
			throw new NonRecoverableError();

		}
	}

	// Return the next valid objectID
	public int getDocumentId() throws NonRecoverableError {

		documentId++;

		// Access the COUNTERS table
		int numUpdatedRows;

		// Update the documentID counter
		try {

			PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_COUNTERS_SET_DOCUMENT_ID);
			preparedStatement.setInt(1, documentId);
			numUpdatedRows = preparedStatement.executeUpdate();

		} catch (SQLException e) {

			System.out.println(e.toString());
			System.out.println(CANNOT_UPDATE_COUNTER.getMessage());
			throw new NonRecoverableError();

		}

		// Check that the update has been effectively completed
		if (numUpdatedRows != 1) {

			System.out.println(CORRUPTED_COUNTER.getMessage());
			throw new NonRecoverableError();

		}

		return documentId;

	}
}
