package sanvito.rest;

import org.junit.Before;
import static org.hamcrest.Matchers.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.google.gson.JsonObject;
import com.hyphenated.card.config.WebConfig;
import com.hyphenated.card.domain.CommonTournamentFormats;

import junit.framework.Assert;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = WebConfig.class)
public class RestTest {

	private static final Logger logger = LogManager.getLogger(RestTest.class);

	@Autowired
	private WebApplicationContext context;

	private MockMvc mvc;

	@Before
	public void setup() {
		mvc = MockMvcBuilders.webAppContextSetup(context).build();
	}

	private String createAGame() {
		try {
			MvcResult result = mvc.perform(MockMvcRequestBuilders.post("/create").param("gameName", "TestGame")
					.param("gameStructure", CommonTournamentFormats.TWO_HR_SEVENPPL.toString())).andReturn();
			String resStr = result.getResponse().getContentAsString();
			String gId = new JSONObject(resStr).get("gameId").toString();
			logger.info("created game with id: " + gId);
			return gId;
		} catch (Exception e) {
			Assert.fail("could not create a game");
		}
		return "NO_GAME";
	}

	private String joinAGame(String gameId, String playerName) {
		try {
			ResultActions ra = mvc.perform(
					MockMvcRequestBuilders.post("/join").param("gameId", gameId).param("playerName", playerName))
					.andExpect(jsonPath("$.playerId", notNullValue()));
			String resStr = ra.andReturn().getResponse().getContentAsString();
			String playerId = new JSONObject(resStr).getString("playerId");
			logger.info("player " + playerId + " joined game " + gameId);
			return playerId;
		} catch (Exception e) {
			Assert.fail("could not join game");
		}
		return "NO_PLAYER";
	}

	private void startAGame(String gameId) {
		try {
			mvc.perform(MockMvcRequestBuilders.post("/startgame").param("gameId", gameId))
					.andExpect(jsonPath("$.success", is(true)));
		} catch (Exception e) {
			Assert.fail("could not start the game.");
		}
	}

	private String startAHand(String gameId) {
		try {
			ResultActions ra = mvc.perform(MockMvcRequestBuilders.post("/starthand").param("gameId", gameId))
					.andExpect(jsonPath("$.handId", notNullValue()));
			return new JSONObject(ra.andReturn().getResponse().getContentAsString()).getString("handId");
		} catch (Exception e) {
			Assert.fail("could not start the hand.");
		}
		return "NO_HAND";
	}

	private JSONObject getPlayerState(String gId, String playerId) throws Exception {
		ResultActions response = mvc.perform(post("/status").param("gameId", gId).param("playerId", playerId));
		return new JSONObject(response.andReturn().getResponse().getContentAsString());
	}

	private void check(String gId, String playerId) throws Exception {
		mvc.perform(post("/check").param("gameId", gId).param("playerId", playerId)).andReturn();
	}

	private JSONObject street(int streetNo, String handId) throws Exception {
		MvcResult result = null;
		switch (streetNo) {
		case 1:
			result = mvc.perform(post("/flop").param("handId", handId)).andReturn();
			return new JSONObject(result.getResponse().getContentAsString());
		case 2:
			result = mvc.perform(post("/turn").param("handId", handId)).andReturn();
			return new JSONObject(result.getResponse().getContentAsString());
		case 3:
			result = mvc.perform(post("/river").param("handId", handId)).andReturn();
			return new JSONObject(result.getResponse().getContentAsString());
		default:
			return null;
		}
	}

	private void endHand(String handId) throws Exception {
		mvc.perform(post("/endhand").param("handId", handId)).andExpect(jsonPath("$.success", is(true)));
	}

	@Test
	public void playHUCheckThough() throws Exception {
		String gId = createAGame();
		String p1Id = joinAGame(gId, "P1");
		String p2Id = joinAGame(gId, "P2");
		startAGame(gId);
		String handId = startAHand(gId);
		check(gId, p1Id);
		check(gId, p2Id);
		JSONObject flop = street(1, handId);
		check(gId, p1Id);
		check(gId, p2Id);
		JSONObject turn = street(2, handId);
		check(gId, p1Id);
		check(gId, p2Id);
		JSONObject river = street(3, handId);
		check(gId, p1Id);
		check(gId, p2Id);
		endHand(handId);
		JSONObject p1State = getPlayerState(gId, p1Id);
		JSONObject p2State = getPlayerState(gId, p2Id);
		logger.info("result: " + "P1 " + p1State.getString("status") + ", " + p1State.getString("card1") + " "
				+ p1State.getString("card2"));
		logger.info("result: " + "P2 " + p2State.getString("status") + ", " + p2State.getString("card1") + " "
				+ p2State.getString("card2"));
		logger.info("whole cards: " + flop.getString("card1") + " " + flop.getString("card2") + " "
				+ flop.getString("card3") + " " + turn.getString("card4") + " " + river.getString("card5"));
	}

}

// JsonObject value = Json.createObjectBuilder()
// .add("firstName", "John")
// .add("lastName", "Smith")
// .add("age", 25)
// .add("address", Json.createObjectBuilder()
// .add("streetAddress", "21 2nd Street")
// .add("city", "New York")
// .add("state", "NY")
// .add("postalCode", "10021"))
// .add("phoneNumber", Json.createArrayBuilder()
// .add(Json.createObjectBuilder()
// .add("type", "home")
// .add("number", "212 555-1234"))
// .add(Json.createObjectBuilder()
// .add("type", "fax")
// .add("number", "646 555-4567")))
// .build();

// Gson gson = new Gson();
// String json = gson.toJson(obj);

// myString = new JSONObject()
// .put("JSON", "Hello, World!").toString();

// mvc.perform(post("/api/todo").
// .andExpect(status().isBadRequest())
// .andExpect(content().mimeType(IntegrationTestUtil.APPLICATION_JSON_UTF8)).andExpect(content()
// .string("{\"fieldErrors\":[{\"path\":\"title\",\"message\":\"The title
// cannot be empty.\"}]}"));
// String s = gson.toJson(g);
//
// PlayerStatus playerStatus =
// gson.fromJson(result.getResponse().getContentAsString(),
// PlayerStatus.class);
// logger.info(playerStatus.getStatus().toString());
