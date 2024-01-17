package behaviourtests;

import org.junit.runner.RunWith;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import io.cucumber.junit.CucumberOptions.SnippetType;

@RunWith(Cucumber.class)
@CucumberOptions(
		plugin="summary",
		snippets = SnippetType.CAMELCASE,
		features="features")
public class CucumberTest {
	//4e0c5084-7227-4f52-a4d9-a522746018fe account created
	//01eb036e-fe15-4f04-93f8-494c0abee3e7 account created
	//
}