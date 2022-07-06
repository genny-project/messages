package life.genny.messages.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.*;
import com.github.jknack.handlebars.context.FieldValueResolver;
import com.github.jknack.handlebars.context.JavaBeanValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;
import com.github.jknack.handlebars.context.MethodValueResolver;
import life.genny.messages.managers.QEmailMessageManager;
import org.jboss.logging.Logger;

import java.util.Base64;
import java.util.Map;

public class MsgUtils {

	private static final Logger log = Logger.getLogger(QEmailMessageManager.class);
	
	public static String encodeUrl(String base, String parentCode, String code, String targetCode) {
		return encodeUrl(base, parentCode, code, targetCode, null);
	}

	/**
	* A Function for Base64 encoding urls
	*
	* @param base			The base of the url that should not be encoded
	* @param parentCode		the parentCode to encode
	* @param code			The code to encode
	* @param targetCode		the targetCode to encode
	* @param token			The token to attach that should not be encoded
	*
	* @return				The comlpete URL
	 */
	public static String encodeUrl(String base, String parentCode, String code, String targetCode, String token) {
		/**
		 * A Function for Base64 encoding urls
		 **/

		// Encode Parent and Code
		String encodedParentCode = new String(Base64.getEncoder().encode(parentCode.getBytes()));
		String encodedCode = new String(Base64.getEncoder().encode(code.getBytes()));
		String url = base + "/" + encodedParentCode + "/" + encodedCode;

		// Add encoded targetCode if not null
		if (targetCode != null) {
			String encodedTargetCode = new String(Base64.getEncoder().encode(targetCode.getBytes()));
			url = url + "/" + encodedTargetCode;
		}

		// Add access token if not null
		if (token != null) {
			url = url +"?token=" + token;
		}
		return url;
	}

	/**
	 * Uses StringBuilder Pattern
	 * @param base
	 * @param parentCode
	 * @param code
	 * @param targetCode
	 * @param token
	 * @return
	 */
	public static String encodedUrlBuilder(String base, String parentCode, String code, String targetCode, String token) {
		/**
		 * A Function for Base64 encoding urls
		 **/
		StringBuilder url = new StringBuilder();
		// Encode Parent and Code
		url.append(base);

		if(parentCode != null){
			url.append("/")
				.append(Base64.getEncoder().encodeToString(parentCode.getBytes()));
		}

		if(code != null){
			url.append("/")
					.append(Base64.getEncoder().encodeToString(code.getBytes()));
		}

		// Add encoded targetCode if not null
		if (targetCode != null) {
			url
					.append("/")
					.append(Base64.getEncoder().encodeToString(targetCode.getBytes()));
		}

		// Add access token if not null
		if (token != null) {
			url
					.append("?token=")
					.append(token);
		}
		return url.toString();
	}

	public static String parseToTemplate(String template, Map<String, Object> data) {
		log.info("Parsing to Template");

		try {
			ObjectMapper objectMapper = new ObjectMapper();
			System.out.println("##### template: " + template);

			JsonNode jsonNode = objectMapper.valueToTree(data);
			Handlebars handlebars = new Handlebars();
			handlebars.registerHelper("json", Jackson2Helper.INSTANCE);

			Context context = Context
					.newBuilder(jsonNode)
					.resolver(
							JsonNodeValueResolver.INSTANCE,
							JavaBeanValueResolver.INSTANCE,
							FieldValueResolver.INSTANCE,
							MapValueResolver.INSTANCE,
							MethodValueResolver.INSTANCE
					)
					.build();
			Template handleBarTemplate = handlebars.compileInline(template);
			String output = handleBarTemplate.apply(context);
			System.out.println("##### parsed template: " + output);
			return output;
		} catch (Exception ex) {
			System.out.println("Exception: " + ex.getMessage());
			return null;
		}
	}


}
