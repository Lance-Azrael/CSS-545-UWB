# EasyScreen Translator

Dingyuan Xue, Sicen Liu 
Version #1

## Summary of Project

Easyscreen Translator offers a one-click feature to translate screen content and supports selecting the specific area of the screen that needs translation.

## Project Analysis

### Value Proposition

Most translation apps require manual text input, which can be tedious when dealing with non-text formats like images on mobile devices. While tools like Google Translate allow photo translation, if you want to translate what's on the screen, the process involves multiple steps—capturing, cropping, switching apps, and importing. EasyScreen Translator streamlines the entire experience into a single step, saving users time and hassle.

### Primary Purpose

EasyScreen translator provides users with a one-click feature to translate screen content.

### Target Audience

The target demographic is beginners in foreign languages, especially university students, who need to read much foreign literature on mobile devices (especially on a tablet computer). University students often need to read PDF papers and formatted web materials, which are inconvenient for copying text. Additionally, for beginners, switching between the original text and the translated content occurs frequently. Promotion can be conducted on commonly used resource-sharing platforms to reach this group.

### Success Criteria

1. It fully realizes the function. 
2. It achieves at least an 80% satisfaction rate in user feedback within the first three months.
3. It attains a conversion rate of at least 10% from free trial users to paid subscribers within the first three months.
4. It reaches a minimum of 1000 unique users within the first year.

### Competitor Analysis

Google Translate offers more features, such as conversation translation and handwriting recognition, as well as additional information about the translated content, like synonyms and example sentences. However, translating screen content in Google involves several complicated steps—capturing, cropping, switching apps, and importing, making it a cumbersome process.

### Monetization Model

Freeminum. Provide users with a certain number of free trial uses, after which they must purchase the software features for continued access.

## Initial Design

### UI/UX Design

Initial page: 1. Permission Button: Click to acquire the permissions such as floating window and background running. 2. Start and Stop Button: Click to start the service, creating a Screenshot button on the screen. Click to disappear the existing Screenshot button. Only active after the user gives the permissions. 3. Setting Button: Click to set the translation language and other options.

On Screen: 1. Screenshot button: Click to take a screenshot and allow the user to clip the area they want to translate. 2. Text area: Show the result of the translation. Only appear after the user has defined the area. 3. Back Button: Click to close the screenshot window or the translation text. 

<img src="https://github.com/Lance-Azrael/CSS-545-UWB/blob/CP1/CP1%20image.jpg?raw=true" alt="img" style="zoom:25%;" />

### Technical Architecture

1. A component to control the screenshot function and clipping function. May use the screenshot function of the system. May store the image in the galley of the system.
2. A component to control recognizing text from the screenshot image. May use an opensource library ([Tess4J](https://github.com/nguyenq/tess4j)) or a ML model.
3. A component to control translating the text. It will be a ML model ([Transformers](https://github.com/huggingface/transformers)), deploying locally for MVP. May use web interaction in the final version to improve the behavior. 

## Challenges and Open Questions

1. The acquisition of permissions such as capturing the screen, floating window, storage and background running. Requesting these permissions when the user clicks the Permission Button. Active the Start and Stop button only after obtaining these permissions. 
2. Getting the area the user wants to translate. Use the screenshot function of the system. May record the position where the user has touched to clip the screenshot image, or use the image edit function of the system. 
3. Privacy protection. Inform users in advance if the content needs to be uploaded. Store all data such as images locally. Do not record anything when the user clicks the screenshot button. 

Question: Does Android allow an app to do the screenshot without using the Android system's own screenshot function? Are there any possible solutions to implement a self-defined screenshot function and are there any apps did this before?
