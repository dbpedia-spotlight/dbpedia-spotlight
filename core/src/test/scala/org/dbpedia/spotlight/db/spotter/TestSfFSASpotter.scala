/**
 *  Copyright 2015 DBpedia Spotlight
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */


package org.dbpedia.spotlight.db.spotter

import java.util.Locale

import org.dbpedia.spotlight.db.memory.MemoryTokenTypeStore
import org.dbpedia.spotlight.db.model.Stemmer
import org.dbpedia.spotlight.db.tokenize.{LanguageIndependentTokenizer, LanguageIndependentStringTokenizer}
import org.dbpedia.spotlight.model.TokenType
import org.dbpedia.spotlight.db.AllOccurrencesFSASpotter
import org.dbpedia.spotlight.db.FSASpotter
import org.junit.Test
import org.junit.Assert.assertEquals

import scala.collection.mutable.ListBuffer

class TestSfFSASpotter{

  val articleText = "Autism's individual symptoms occur in the general population and appear not to associate highly, without a sharp line " +
    "separating pathologically severe from common traits.Social deficits distinguish autism and the related autism spectrum disorders from other developmental disorders. " +
    "People with autism have social impairments and often lack the intuition about others that many people take for granted. Noted autistic Temple Grandin described her " +
    "inability to understand the social communication of neurotypicals, or people with normal neural development, as leaving her feeling \\\"like an anthropologist on " +
    "Mars\\\".Unusual social development becomes apparent early in childhood. Autistic infants show less attention to social stimuli, smile and look at others less often, " +
    "and respond less to their own name. Autistic toddlers differ more strikingly from social norms; for example, they have less eye contact and turn taking, and do not " +
    "have the ability to use simple movements to express themselves, such as the deficiency to point at things. Three- to five-year-old children with autism are less likely to " +
    "exhibit social understanding, approach others spontaneously, imitate and respond to emotions, communicate nonverbally, and take turns with others. However, they do form " +
    "attachments to their primary caregivers. Most childen with autism display moderately less attachment security than neurotypical children, although this difference " +
    "disappears in children with higher mental development or less severe ASD. Older children and adults with ASD perform worse on tests of face and emotion recognition." +
    "Children with high-functioning autism suffer from more intense and frequent loneliness compared to non-autistic peers, despite the common belief that children with " +
    "autism prefer to be alone. Making and maintaining friendships often proves to be difficult for those with autism. For them, the quality of friendships, not the number " +
    "of friends, predicts how lonely they feel. Functional friendships, such as those resulting in invitations to parties, may affect the quality of life more deeply.There " +
    "are many anecdotal reports, but few systematic studies, of aggression and violence in individuals with ASD. The limited data suggest that, in children with intellectual " +
    "disability, autism is associated with aggression, destruction of property, and tantrums.About a third to a half of individuals with autism do not develop enough natural " +
    "speech to meet their daily communication needs. Differences in communication may be present from the first year of life, and may include delayed onset of babbling, unusual " +
    "gestures, diminished responsiveness, and vocal patterns that are not synchronized with the caregiver. In the second and third years, children with autism have less frequent " +
    "and less diverse babbling, consonants, words, and word combinations; their gestures are less often integrated with words. Children with autism are less likely to make " +
    "requests or share experiences, and are more likely to simply repeat others' words or reverse pronouns. Joint attention seems to be necessary for functional speech, and " +
    "deficits in joint attention seem to distinguish infants with ASD: for example, they may look at a pointing hand instead of the pointed-at object, and they consistently " +
    "fail to point at objects in order to comment on or share an experience. Children with autism may have difficulty with imaginative play and with developing symbols into " +
    "language.In a pair of studies, high-functioning children with autism aged 8–15 performed equally well as, and adults better than, individually matched controls at basic " +
    "language tasks involving vocabulary and spelling. Both autistic groups performed worse than controls at complex language tasks such as figurative language, comprehension " +
    "and inference. As people are often sized up initially from their basic language skills, these studies suggest that people speaking to autistic individuals are more likely " +
    "to overestimate what their audience comprehends.Autistic individuals display many forms of repetitive or restricted behavior, which the Repetitive Behavior Scale-Revised " +
    "categorizes as follows.No single repetitive or self-injurious behavior seems to be specific to autism, but only autism appears to have an elevated pattern of occurrence " +
    "and severity of these behaviors.Autistic individuals may have symptoms that are independent of the diagnosis, but that can affect the individual or the family. An estimated " +
    "0.5% to 10% of individuals with ASD show unusual abilities, ranging from splinter skills such as the memorization of trivia to the extraordinarily rare talents of prodigious " +
    "autistic savants. Many individuals with ASD show superior skills in perception and attention, relative to the general population. Sensory abnormalities are found in over 90% " +
    "of those with autism, and are considered core features by some, although there is no good evidence that sensory symptoms differentiate autism from other developmental disorders. " +
    "Differences are greater for under-responsivity than for over-responsivity or for sensation seeking . An estimated 60%–80% of autistic people have motor signs that include poor " +
    "muscle tone, poor motor planning, and toe walking; deficits in motor coordination are pervasive across ASD and are greater in autism proper.Unusual eating behavior occurs " +
    "in about three-quarters of children with ASD, to the extent that it was formerly a diagnostic indicator. Selectivity is the most common problem, although eating rituals and " +
    "food refusal also occur; this does not appear to result in malnutrition. Although some children with autism also have gastrointestinal symptoms, there is a lack of published " +
    "rigorous data to support the theory that children with autism have more or different GI symptoms than usual; studies report conflicting results, and the relationship between " +
    "GI problems and ASD is unclear.Parents of children with ASD have higher levels of stress. Siblings of children with ASD report greater admiration of and less conflict with the " +
    "affected sibling than siblings of unaffected children and were similar to siblings of children with Down syndrome in these aspects of the sibling relationship. However, they " +
    "reported lower levels of closeness and intimacy than siblings of children with Down syndrome; siblings of individuals with ASD have greater risk of negative well-being and " +
    "poorer sibling relationships as adults.It has long been presumed that there is a common cause at the genetic, cognitive, and neural levels for autism's characteristic triad " +
    "of symptoms. However, there is increasing suspicion that autism is instead a complex disorder whose core aspects have distinct causes that often co-occur.Autism has a strong " +
    "genetic basis, although the genetics of autism are complex and it is unclear whether ASD is explained more by rare mutations with major effects, or by rare multigene interactions " +
    "of common genetic variants. Complexity arises due to interactions among multiple genes, the environment, and epigenetic factors which do not change DNA but are heritable and " +
    "influence gene expression. Studies of twins suggest that heritability is 0.7 for autism and as high as 0.9 for ASD, and siblings of those with autism are about 25 times more " +
    "likely to be autistic than the general population. However, most of the mutations that increase autism risk have not been identified. Typically, autism cannot be traced to a " +
    "Mendelian mutation or to a single chromosome abnormality, and none of the genetic syndromes associated with ASDs have been shown to selectively cause ASD. Numerous candidate " +
    "genes have been located, with only small effects attributable to any particular gene. The large number of autistic individuals with unaffected family members may result from " +
    "copy number variations—spontaneous deletions or duplications in genetic material during meiosis. Hence, a substantial fraction of autism cases may be traceable to genetic " +
    "causes that are highly heritable but not inherited: that is, the mutation that causes the autism is not present in the parental genome.Several lines of evidence point to synaptic " +
    "dysfunction as a cause of autism. Some rare mutations may lead to autism by disrupting some synaptic pathways, such as those involved with cell adhesion. Gene replacement " +
    "studies in mice suggest that autistic symptoms are closely related to later developmental steps that depend on activity in synapses and on activity-dependent changes. " +
    "All known teratogens related to the risk of autism appear to act during the first eight weeks from conception, and though this does not exclude the possibility that autism " +
    "can be initiated or affected later, there is strong evidence that autism arises very early in development.Exposure to air pollution during pregnancy, especially heavy" +
    " metals and particulates, may increase the risk of autism. Environmental factors that have been claimed to contribute to or exacerbate autism, or may be important in future " +
    "research, include certain foods, infectious diseases, solvents, diesel exhaust, PCBs, phthalates and phenols used in plastic products, pesticides, brominated flame retardants, " +
    "alcohol, smoking, illicit drugs, vaccines, and prenatal stress, although no links have been found, and some have been completely disproven.Parents may first become aware of " +
    "autistic symptoms in their child around the time of a routine vaccination. This has led to unsupported theories blaming vaccine \\\"overload\\\", a vaccine preservative, or the " +
    "MMR vaccine for causing autism. The latter theory was supported by a litigation-funded study that has since been shown to have been \\\"an elaborate fraud\\\". Although these " +
    "theories lack convincing scientific evidence and are biologically implausible, parental concern about a potential vaccine link with autism has led to lower rates of childhood " +
    "immunizations, outbreaks of previously controlled childhood diseases in some countries, and the preventable deaths of several children.Autism's symptoms result from " +
    "maturation-related changes in various systems of the brain. How autism occurs is not well understood. Its mechanism can be divided into two areas: the pathophysiology of brain " +
    "structures and processes associated with autism, and the neuropsychological linkages between brain structures and behaviors. The behaviors appear to have multiple " +
    "pathophysiologies.Unlike many other brain disorders, such as Parkinson's, autism does not have a clear unifying mechanism at either the molecular, cellular, or systems level; " +
    "it is not known whether autism is a few disorders caused by mutations converging on a few common molecular pathways, or is a large set of disorders with diverse mechanisms. " +
    "Autism appears to result from developmental factors that affect many or all functional brain systems, and to disturb the timing of brain development more than the final product. " +
    "Neuroanatomical studies and the associations with teratogens strongly suggest that autism's mechanism includes alteration of brain development soon after conception. This anomaly " +
    "appears to start a cascade of pathological events in the brain that are significantly influenced by environmental factors. Just after birth, the brains of children with autism " +
    "tend to grow faster than usual, followed by normal or relatively slower growth in childhood. It is not known whether early overgrowth occurs in all children with autism. It seems " +
    "to be most prominent in brain areas underlying the development of higher cognitive specialization. Hypotheses for the cellular and molecular bases of pathological early overgrowth " +
    "include the following:The immune system is thought to play an important role in autism. Children with autism have been found by researchers to have inflammation of both the " +
    "peripheral and central immune systems as indicated by increased levels of pro-inflammatory cytokines and significant activation of microglia. Biomarkers of abnormal immune function " +
    "have also been associated with increased impairments in behaviors that are characteristic of the core features of autism such as deficits in social interactions and " +
    "communication. Interactions between the immune system and the nervous system begin early during the embryonic stage of life, and successful neurodevelopment depends on a " +
    "balanced immune response. It is thought that activation of a pregnant mother's immune system such as from environmental toxicants or infection can contribute to causing " +
    "autism through causing a disruption of brain development. This is supported by recent studies that have found that infection during pregnancy is associated with an increased " +
    "risk of autism.The relationship of neurochemicals to autism is not well understood; several have been investigated, with the most evidence for the role of serotonin and of " +
    "genetic differences in its transport. The role of group I metabotropic glutamate receptors in the pathogenesis of fragile X syndrome, the most common identified genetic cause " +
    "of autism, has led to interest in the possible implications for future autism research into this pathway. Some data suggests neuronal overgrowth potentially related to an " +
    "increase in several growth hormones or to impaired regulation of growth factor receptors. Also, some inborn errors of metabolism are associated with autism, but probably " +
    "account for less than 5% of cases.The mirror neuron system theory of autism hypothesizes that distortion in the development of the MNS interferes with imitation and leads to " +
    "autism's core features of social impairment and communication difficulties. The MNS operates when an animal performs an action or observes another animal perform the same action. " +
    "The MNS may contribute to an individual's understanding of other people by enabling the modeling of their behavior via embodied simulation of their actions, intentions, and " +
    "emotions. Several studies have tested this hypothesis by demonstrating structural abnormalities in MNS regions of individuals with ASD, delay in the activation in the core " +
    "circuit for imitation in individuals with Asperger syndrome, and a correlation between reduced MNS activity and severity of the syndrome in children with ASD. However, " +
    "individuals with autism also have abnormal brain activation in many circuits outside the MNS and the MNS theory does not explain the normal performance of children with " +
    "autism on imitation tasks that involve a goal or object.ASD-related patterns of low function and aberrant activation in the brain differ depending on whether the brain is " +
    "doing social or nonsocial tasks. In autism there is evidence for reduced functional connectivity of the default network, a large-scale brain network involved in social and " +
    "emotional processing, with intact connectivity of the task-positive network, used in sustained attention and goal-directed thinking. In people with autism the two networks " +
    "are not negatively correlated in time, suggesting an imbalance in toggling between the two networks, possibly reflecting a disturbance of self-referential thought." +
    "The underconnectivity theory of autism hypothesizes that autism is marked by underfunctioning high-level neural connections and synchronization, along with an excess of " +
    "low-level processes. Evidence for this theory has been found in functional neuroimaging studies on autistic individuals and by a brainwave study that suggested that adults " +
    "with ASD have local overconnectivity in the cortex and weak functional connections between the frontal lobe and the rest of the cortex. Other evidence suggests the " +
    "underconnectivity is mainly within each hemisphere of the cortex and that autism is a disorder of the association cortex.From studies based on event-related potentials, " +
    "transient changes to the brain's electrical activity in response to stimuli, there is considerable evidence for differences in autistic individuals with respect to attention, " +
    "orientation to auditory and visual stimuli, novelty detection, language and face processing, and information storage; several studies have found a preference for " +
    "nonsocial stimuli. For example, magnetoencephalography studies have found evidence in children with autism of delayed responses in the brain's processing of auditory " +
    "signals.In the genetic area, relations have been found between autism and schizophrenia based on duplications and deletions of chromosomes; research showed that schizophrenia " +
    "and autism are significantly more common in combination with 1q21.1 deletion syndrome. Research on autism/schizophrenia relations for chromosome 15 , chromosome 16 and " +
    "chromosome 17 are inconclusive.Two major categories of cognitive theories have been proposed about the links between autistic brains and behavior.The first category focuses " +
    "on deficits in social cognition. Simon Baron-Cohen's empathizing–systemizing theory postulates that autistic individuals can systemize—that is, they can develop internal rules " +
    "of operation to handle events inside the brain—but are less effective at empathizing by handling events generated by other agents. An extension, the extreme male brain theory, " +
    "hypothesizes that autism is an extreme case of the male brain, defined psychometrically as individuals in whom systemizing is better than empathizing. These theories are " +
    "somewhat related to Baron-Cohen's earlier theory of mind approach, which hypothesizes that autistic behavior arises from an inability to ascribe mental states to oneself and " +
    "others. The theory of mind hypothesis is supported by the atypical responses of children with autism to the Sally–Anne test for reasoning about others' motivations, " +
    "and the mirror neuron system theory of autism described in Pathophysiology maps well to the hypothesis. However, most studies have found no evidence of impairment in " +
    "autistic individuals' ability to understand other people's basic intentions or goals; instead, data suggests that impairments are found in understanding more complex social " +
    "emotions or in considering others' viewpoints.The second category focuses on nonsocial or general processing: the executive functions such as working memory, planning, " +
    "inhibition. In his review, Kenworthy states that \\\"the claim of executive dysfunction as a causal factor in autism is controversial\\\", however, \\\"it is clear that " +
    "executive dysfunction plays a role in the social and cognitive deficits observed in individuals with autism\\\". Tests of core executive processes such as eye movement " +
    "tasks indicate improvement from late childhood to adolescence, but performance never reaches typical adult levels. A strength of the theory is predicting stereotyped " +
    "behavior and narrow interests; two weaknesses are that executive function is hard to measure and that executive function deficits have not been found in young children with " +
    "autism.Weak central coherence theory hypothesizes that a limited ability to see the big picture underlies the central disturbance in autism. One strength of this theory is " +
    "predicting special talents and peaks in performance in autistic people. A related theory—enhanced perceptual functioning—focuses more on the superiority of locally oriented " +
    "and perceptual operations in autistic individuals. These theories map well from the underconnectivity theory of autism.Neither category is satisfactory on its own; " +
    "social cognition theories poorly address autism's rigid and repetitive behaviors, while the nonsocial theories have difficulty explaining social impairment and communication " +
    "difficulties. A combined theory based on multiple deficits may prove to be more useful.Diagnosis is based on behavior, not cause or mechanism. Under the DSM-5, autism is " +
    "characterized by persistent deficits in social communication and interaction across multiple contexts, as well as restricted, repetitive patterns of behavior, interests, or " +
    "activities. These deficits are present in early childhood, typically before age three, and lead to clinically significant functional impairment. Sample symptoms include lack " +
    "of social or emotional reciprocity, stereotyped and repetitive use of language or idiosyncratic language, and persistent preoccupation with unusual objects. The disturbance " +
    "must not be better accounted for by Rett syndrome, intellectual disability or global developmental delay. ICD-10 uses essentially the same definition.Several diagnostic " +
    "instruments are available. Two are commonly used in autism research: the Autism Diagnostic Interview-Revised is a semistructured parent interview, and the Autism Diagnostic " +
    "Observation Schedule uses observation and interaction with the child. The Childhood Autism Rating Scale is used widely in clinical environments to assess severity of autism " +
    "based on observation of children.A pediatrician commonly performs a preliminary investigation by taking developmental history and physically examining the child. If warranted, " +
    "diagnosis and evaluations are conducted with help from ASD specialists, observing and assessing cognitive, communication, family, and other factors using standardized tools, " +
    "and taking into account any associated medical conditions. A pediatric neuropsychologist is often asked to assess behavior and cognitive skills, both to aid diagnosis and to " +
    "help recommend educational interventions. A differential diagnosis for ASD at this stage might also consider intellectual disability, hearing impairment, and a specific " +
    "language impairment such as Landau–Kleffner syndrome. The presence of autism can make it harder to diagnose coexisting psychiatric disorders such as depression.Clinical " +
    "genetics evaluations are often done once ASD is diagnosed, particularly when other symptoms already suggest a genetic cause. Although genetic technology allows clinical " +
    "geneticists to link an estimated 40% of cases to genetic causes, consensus guidelines in the US and UK are limited to high-resolution chromosome and fragile X testing. " +
    "A genotype-first model of diagnosis has been proposed, which would routinely assess the genome's copy number variations. As new genetic tests are developed several ethical, " +
    "legal, and social issues will emerge. Commercial availability of tests may precede adequate understanding of how to use test results, given the complexity of autism's genetics. " +
    "Metabolic and neuroimaging tests are sometimes helpful, but are not routine.ASD can sometimes be diagnosed by age 14 months, although diagnosis becomes increasingly " +
    "stable over the first three years of life: for example, a one-year-old who meets diagnostic criteria for ASD is less likely than a three-year-old to continue to do so a " +
    "few years later. In the UK the National Autism Plan for Children recommends at most 30 weeks from first concern to completed diagnosis and assessment, though few cases are " +
    "handled that quickly in practice. Although the symptoms of autism and ASD begin early in childhood, they are sometimes missed; years later, adults may seek diagnoses to help " +
    "them or their friends and family understand themselves, to help their employers make adjustments, or in some locations to claim disability living allowances or other benefits." +
    "Underdiagnosis and overdiagnosis are problems in marginal cases, and much of the recent increase in the number of reported ASD cases is likely due to changes in diagnostic " +
    "practices. The increasing popularity of drug treatment options and the expansion of benefits has given providers incentives to diagnose ASD, resulting in some overdiagnosis " +
    "of children with uncertain symptoms. Conversely, the cost of screening and diagnosis and the challenge of obtaining payment can inhibit or delay diagnosis. It is particularly " +
    "hard to diagnose autism among the visually impaired, partly because some of its diagnostic criteria depend on vision, and partly because autistic symptoms overlap with those " +
    "of common blindness syndromes or blindisms.Autism is one of the five pervasive developmental disorders , which are characterized by widespread abnormalities of social " +
    "interactions and communication, and severely restricted interests and highly repetitive behavior. These symptoms do not imply sickness, fragility, or emotional disturbance.Of " +
    "the five PDD forms, Asperger syndrome is closest to autism in signs and likely causes; Rett syndrome and childhood disintegrative disorder share several signs with autism, but " +
    "may have unrelated causes; PDD not otherwise specified is diagnosed when the criteria are not met for a more specific disorder. Unlike with autism, people with " +
    "Asperger syndrome have no substantial delay in language development. The terminology of autism can be bewildering, with autism, Asperger syndrome and PDD-NOS often " +
    "called the autism spectrum disorders or sometimes the autistic disorders, whereas autism itself is often called autistic disorder,childhood autism, or infantile autism. " +
    "In this article, autism refers to the classic autistic disorder; in clinical practice, though, autism, ASD, and PDD are often used interchangeably. ASD, in turn, is a " +
    "subset of the broader autism phenotype, which describes individuals who may not have ASD but do have autistic-like traits, such as avoiding eye contact.The manifestations of " +
    "autism cover a wide spectrum, ranging from individuals with severe impairments—who may be silent, developmentally disabled, and locked into hand flapping and rocking—to high " +
    "functioning individuals who may have active but distinctly odd social approaches, narrowly focused interests, and verbose, pedantic communication. Because the behavior spectrum " +
    "is continuous, boundaries between diagnostic categories are necessarily somewhat arbitrary. Sometimes the syndrome is divided into low-, medium- or high-functioning autism , " +
    "based on IQ thresholds, or on how much support the individual requires in daily life; these subdivisions are not standardized and are controversial. Autism can also be divided " +
    "into syndromal and non-syndromal autism; the syndromal autism is associated with severe or profound intellectual disability or a congenital syndrome with physical symptoms, " +
    "such as tuberous sclerosis. Although individuals with Asperger syndrome tend to perform better cognitively than those with autism, the extent of the overlap between " +
    "Asperger syndrome, HFA, and non-syndromal autism is unclear.Some studies have reported diagnoses of autism in children due to a loss of language or social skills, as " +
    "opposed to a failure to make progress, typically from 15 to 30 months of age. The validity of this distinction remains controversial; it is possible that regressive autism " +
    "is a specific subtype, or that there is a continuum of behaviors between autism with and without regression.Research into causes has been hampered by the inability to " +
    "identify biologically meaningful subgroups within the autistic population and by the traditional boundaries between the disciplines of psychiatry, psychology, neurology and " +
    "pediatrics. Newer technologies such as fMRI and diffusion tensor imaging can help identify biologically relevant phenotypes that can be viewed on brain scans, to help further " +
    "neurogenetic studies of autism; one example is lowered activity in the fusiform face area of the brain, which is associated with impaired perception of people versus objects. " +
    "It has been proposed to classify autism using genetics as well as behavior.About half of parents of children with ASD notice their child's unusual behaviors by age 18 months, " +
    "and about four-fifths notice by age 24 months. According to an article in the Journal of Autism and Developmental Disorders, failure to meet any of the following " +
    "milestones \\\"is an absolute indication to proceed with further evaluations. Delay in referral for such testing may delay early diagnosis and treatment and affect " +
    "the long-term outcome\\\".US and Japanese practice is to screen all children for ASD at 18 and 24 months, using autism-specific formal screening tests. In contrast, " +
    "in the UK, children whose families or doctors recognize possible signs of autism are screened. It is not known which approach is more effective. Screening tools include " +
    "the Modified Checklist for Autism in Toddlers , the Early Screening of Autistic Traits Questionnaire, and the First Year Inventory; initial data on M-CHAT and its predecessor " +
    "CHAT on children aged 18–30 months suggests that it is best used in a clinical setting and that it has low sensitivity but good specificity . It may be more accurate to " +
    "precede these tests with a broadband screener that does not distinguish ASD from other developmental disorders. Screening tools designed for one culture's norms for behaviors " +
    "like eye contact may be inappropriate for a different culture. Although genetic screening for autism is generally still impractical, it can be considered in some cases, " +
    "such as children with neurological symptoms and dysmorphic features.Infection with rubella during pregnancy is the cause of about 0.75% of cases of autism. Vaccination " +
    "against rubella can prevent many of these cases.The main goals when treating children with autism are to lessen associated deficits and family distress, and to increase " +
    "quality of life and functional independence. No single treatment is best and treatment is typically tailored to the child's needs. Families and the educational system are " +
    "the main resources for treatment. Studies of interventions have methodological problems that prevent definitive conclusions about efficacy. Although many psychosocial " +
    "interventions have some positive evidence, suggesting that some form of treatment is preferable to no treatment, the methodological quality of systematic reviews of these " +
    "studies has generally been poor, their clinical results are mostly tentative, and there is little evidence for the relative effectiveness of treatment options. Intensive, " +
    "sustained special education programs and behavior therapy early in life can help children acquire self-care, social, and job skills, and often improve functioning and decrease " +
    "symptom severity and maladaptive behaviors; claims that intervention by around age three years is crucial are not substantiated. Available approaches include applied behavior " +
    "analysis , developmental models, structured teaching, speech and language therapy, social skills therapy, and occupational therapy. There is some evidence that early intensive " +
    "behavioral intervention, an early intervention model for 20 to 40;nbsp;hours a week for multiple years, is an effective behavioral treatment for some children " +
    "with ASD.Educational interventions can be effective to varying degrees in most children: intensive ABA treatment has demonstrated effectiveness in enhancing global functioning " +
    "in preschool children and is well-established for improving intellectual performance of young children. Neuropsychological reports are often poorly communicated to educators, " +
    "resulting in a gap between what a report recommends and what education is provided. It is not known whether treatment programs for children lead to significant improvements " +
    "after the children grow up, and the limited research on the effectiveness of adult residential programs shows mixed results. The appropriateness of including children with " +
    "varying severity of autism spectrum disorders in the general education population is a subject of current debate among educators and researchers.Many medications are used to " +
    "treat ASD symptoms that interfere with integrating a child into home or school when behavioral treatment fails. More than half of US children diagnosed with ASD are prescribed " +
    "psychoactive drugs or anticonvulsants, with the most common drug classes being antidepressants, stimulants, and antipsychotics. Aside from antipsychotics, both aripiprazole and " +
    "risperidone are effective for treating irritability in children with autistic disorders. There is scant reliable research about the effectiveness or safety of drug treatments " +
    "for adolescents and adults with ASD. A person with ASD may respond atypically to medications, the medications can have adverse effects, and no known medication relieves autism's " +
    "core symptoms of social and communication impairments. Experiments in mice have reversed or reduced some symptoms related to autism by replacing or modulating gene function, " +
    "suggesting the possibility of targeting therapies to specific rare mutations known to cause autism.Although many alternative therapies and interventions are available, " +
    "few are supported by scientific studies. Treatment approaches have little empirical support in quality-of-life contexts, and many programs focus on success measures that " +
    "lack predictive validity and real-world relevance. Scientific evidence appears to matter less to service providers than program marketing, training availability, " +
    "and parent requests. Some alternative treatments may place the child at risk. A 2008 study found that compared to their peers, autistic boys have significantly thinner " +
    "bones if on casein-free diets; in 2005, botched chelation therapy killed a five-year-old child with autism. There has been early research looking at hyperbaric treatments " +
    "in children with autism.Treatment is expensive; indirect costs are more so. For someone born in 2000, a US study estimated an average lifetime cost of $ , with about 10% " +
    "medical care, 30% extra education and other care, and 60% lost economic productivity. Publicly supported programs are often inadequate or inappropriate for a given child, and " +
    "unreimbursed out-of-pocket medical or therapy expenses are associated with likelihood of family financial problems; one 2008 US study found a 14% average loss of annual income in " +
    "families of children with ASD, and a related study found that ASD is associated with higher probability that child care problems will greatly affect parental employment. " +
    "US states increasingly require private health insurance to cover autism services, shifting costs from publicly funded education programs to privately funded health insurance. " +
    "After childhood, key treatment issues include residential care, job training and placement, sexuality, social skills, and estate planning.There is no known cure. " +
    "Children recover occasionally, so that they lose their diagnosis of ASD; this occurs sometimes after intensive treatment and sometimes not. It is not known how often recovery " +
    "happens; reported rates in unselected samples of children with ASD have ranged from 3% to 25%. Most children with autism acquire language by age five or younger, though a few " +
    "have developed communication skills in later years. Most children with autism lack social support, meaningful relationships, future employment opportunities or self-determination. " +
    "Although core difficulties tend to persist, symptoms often become less severe with age.Few high-quality studies address long-term prognosis. Some adults show modest " +
    "improvement in communication skills, but a few decline; no study has focused on autism after midlife. Acquiring language before age six, having an IQ above 50, and having a " +
    "marketable skill all predict better outcomes; independent living is unlikely with severe autism. Most, but not all, people with autism face significant obstacles in transitioning " +
    "to adulthood.Most recent reviews tend to estimate a prevalence of 1–2 per 1,000 for autism and close to 6 per 1,000 for ASD, and 11 per 1,000 children in the United States for ASD " +
    "as of 2008; because of inadequate data, these numbers may underestimate ASD's true rate. In 2012, the NHS estimated that the overall prevalence of autism among adults aged 18 years " +
    "and over in the UK was 1.1%. Rates of PDD-NOS's has been estimated at 3.7 per 1,000, Asperger syndrome at roughly 0.6 per 1,000, and childhood disintegrative disorder " +
    "at 0.02 per 1,000.The number of reported cases of autism increased dramatically in the 1990s and early 2000s. This increase is largely attributable to changes in diagnostic " +
    "practices, referral patterns, availability of services, age at diagnosis, and public awareness, though unidentified environmental risk factors cannot be ruled out. " +
    "The available evidence does not rule out the possibility that autism's true prevalence has increased; a real increase would suggest directing more attention and funding " +
    "toward changing environmental factors instead of continuing to focus on genetics.Boys are at higher risk for ASD than girls. The sex ratio averages 4.3:1 and is greatly " +
    "modified by cognitive impairment: it may be close to 2:1 with intellectual disability and more than 5.5:1 without. Several theories about the higher prevalence in males " +
    "have been investigated, but the cause of the difference is unconfirmed; one theory is that females are underdiagnosed.Although the evidence does not implicate any single " +
    "pregnancy-related risk factor as a cause of autism, the risk of autism is associated with advanced age in either parent, and with diabetes, bleeding, and use of psychiatric " +
    "drugs in the mother during pregnancy. The risk is greater with older fathers than with older mothers; two potential explanations are the known increase in mutation burden " +
    "in older sperm, and the hypothesis that men marry later if they carry genetic liability and show some signs of autism. Most professionals believe that race, ethnicity, and " +
    "socioeconomic background do not affect the occurrence of autism.Several other conditions are common in children with autism. They include:A few examples of autistic symptoms " +
    "and treatments were described long before autism was named. The Table Talk of Martin Luther, compiled by his notetaker, Mathesius, contains the story of a 12-year-old boy who " +
    "may have been severely autistic. Luther reportedly thought the boy was a soulless mass of flesh possessed by the devil, and suggested that he be suffocated, although a later " +
    "critic has cast doubt on the veracity of this report. The earliest well-documented case of autism is that of Hugh Blair of Borgue, as detailed in a 1747 court case in which his " +
    "brother successfully petitioned to annul Blair's marriage to gain Blair's inheritance. The Wild Boy of Aveyron, a feral child caught in 1798, showed several signs of autism; " +
    "the medical student Jean Itard treated him with a behavioral program designed to help him form social attachments and to induce speech via imitation.The New Latin word autismus " +
    "was coined by the Swiss psychiatrist Eugen Bleuler in 1910 as he was defining symptoms of schizophrenia. He derived it from the Greek word autós , and used it to mean morbid " +
    "self-admiration, referring to \\\"autistic withdrawal of the patient to his fantasies, against which any influence from outside becomes an intolerable disturbance\\\".The word " +
    "autism first took its modern sense in 1938 when Hans Asperger of the Vienna University Hospital adopted Bleuler's terminology autistic psychopaths in a lecture in German about " +
    "child psychology. Asperger was investigating an ASD now known as Asperger syndrome, though for various reasons it was not widely recognized as a separate diagnosis until 1981. " +
    "Leo Kanner of the Johns Hopkins Hospital first used autism in its modern sense in English when he introduced the label early infantile autism in a 1943 report of 11 children " +
    "with striking behavioral similarities. Almost all the characteristics described in Kanner's first paper on the subject, notably \\\"autistic aloneness\\\" and \\\"insistence " +
    "on sameness\\\", are still regarded as typical of the autistic spectrum of disorders. It is not known whether Kanner derived the term independently of Asperger.Kanner's reuse " +
    "of autism led to decades of confused terminology like infantile schizophrenia, and child psychiatry's focus on maternal deprivation led to misconceptions of autism as an infant's " +
    "response to \\\"refrigerator mothers\\\". Starting in the late 1960s autism was established as a separate syndrome by demonstrating that it is lifelong, distinguishing it from " +
    "intellectual disability and schizophrenia and from other developmental disorders, and demonstrating the benefits of involving parents in active programs of therapy. As late as " +
    "the mid-1970s there was little evidence of a genetic role in autism; now it is thought to be one of the most heritable of all psychiatric conditions. Although the rise of parent " +
    "organizations and the destigmatization of childhood ASD have deeply affected how we view ASD, parents continue to feel social stigma in situations where their child's autistic " +
    "behavior is perceived negatively by others, and many primary care physicians and medical specialists still express some beliefs consistent with outdated autism research.The Internet " +
    "has helped autistic individuals bypass nonverbal cues and emotional sharing that they find so hard to deal with, and has given them a way to form online communities and work " +
    "remotely. Sociological and cultural aspects of autism have developed: some in the community seek a cure, while others believe that autism is simply another way of being."

  val sfs = List("agents that cause birth defects",
                 "Controversies",
                 "causes",
                 "diagnostic criteria",
                 "brain",
                 "nerve cell",
                 "synapse",
                 "autism spectrum",
                 "Asperger syndrome",
                 "pervasive developmental disorder, not otherwise specified")
  val lang = "en"


  @Test
  def testSfsinText(){
    val stemmer = new Stemmer()
    val locale = new Locale("en")

    //Initializing Set Iterable
    val lst = new LanguageIndependentStringTokenizer(locale, stemmer)
    val token = sfs.flatMap( sf => lst.tokenizeUnstemmed(sf) ).toSet

    val tokenTypes = new ListBuffer[TokenType]()
    var i= 1
    token.foreach(x =>{
      val token = new TokenType(i,x,0)
      tokenTypes += token
      i += 1

    } )

    //Initalizing the Memory Token Store
    val tokenTypeStore = TestSfFSASpotter.createTokenTypeStore(tokenTypes.toList)

    //Creating a sample StopWords
    val stopWords = Set[String]("a","the","an","that")
    val lit = new LanguageIndependentTokenizer(stopWords,stemmer,locale,tokenTypeStore)

    //Building Dictionary and calling the extract method
    val allOccFSASpotter = new AllOccurrencesFSASpotter(FSASpotter.buildDictionaryFromIterable(sfs,lit),lit)


    allOccFSASpotter.extract(articleText).foreach(sfOffset => {
      val surfaceForm = sfOffset._1.toString
      assertEquals(articleText.substring(sfOffset._2,(sfOffset._2 + surfaceForm.length)),surfaceForm)
    })

  }
}


object TestSfFSASpotter {

  def createTokenTypeStore(tokenTypes:List[TokenType]): MemoryTokenTypeStore =  {

    val tokenTypeStore = new MemoryTokenTypeStore()
    val tokens = new Array[String](tokenTypes.size + 1)
    val counts = new Array[Int](tokenTypes.size + 1)

    tokenTypes.foreach(token => {
      tokens(token.id) = token.tokenType
      counts(token.id) = token.count

    })

    tokenTypeStore.tokenForId  = tokens.array
    tokenTypeStore.counts = counts.array
    tokenTypeStore.loaded()

    return tokenTypeStore
  }
}

